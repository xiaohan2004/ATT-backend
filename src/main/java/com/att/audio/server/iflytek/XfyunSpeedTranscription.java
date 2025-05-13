package com.att.audio.server.iflytek;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.att.audio.server.request.*;
import com.att.audio.server.util.*;
import com.google.gson.Gson;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Builder
public class XfyunSpeedTranscription {
    private static final String FILE_URL_PREFIX = "https://upload-ost-api.xfyun.cn/file";
    private static final String OPEN_URL_PREFIX = "https://ost-api.xfyun.cn/v2";
    private static final int SLICE_SIZE = 15728640; // 15M
    private static final long QUERY_INTERVAL = 2000; // 2秒
    private static final int MAX_WAIT_TIME = 300000; // 5分钟

    private final String appId;
    private final String apiKey;
    private final String apiSecret;
    private final OkHttpClient client;
    private final Gson gson;

    public String convertToText(File audioFile) throws IOException, InterruptedException {
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            // 1. 上传文件
            FileCaller fileCaller = FileCaller.builder()
                    .apiKey(apiKey)
                    .apiSecret(apiSecret)
                    .client(client)
                    .ulrPrefix(FILE_URL_PREFIX)
                    .build();

            JSONObject uploadResult;
            if (audioFile.length() < 31457280) { // 30M
                uploadResult = uploadSmallFile(fileCaller, audioFile, fis);
            } else {
                uploadResult = uploadLargeFile(fileCaller, audioFile, fis);
            }
            String audioUrl = uploadResult.getString("url");
            log.info("文件上传成功，URL：{}", audioUrl);

            // 2. 创建转写任务
            OpenCaller openCaller = OpenCaller.builder()
                    .apiKey(apiKey)
                    .apiSecret(apiSecret)
                    .client(client)
                    .ulrPrefix(OPEN_URL_PREFIX)
                    .build();

            OpenResp createResp = openCaller.create(OpenReq.Create.builder()
                    .common(OpenReq.Common.builder().appId(appId).build())
                    .business(OpenReq.Business.builder()
                            .requestId(String.valueOf(System.currentTimeMillis()))
                            .accent("mandarin")
                            .language("zh_cn")
                            .domain("pro_ost_ed")
                            .build())
                    .data(OpenReq.Data.builder()
                            .audioUrl(audioUrl)
                            .encoding("raw")
                            .format("audio/L16;rate=16000")
                            .audioSrc("http")
                            .build())
                    .build());

            JSONObject createData = JSON.parseObject(JSON.toJSONString(createResp.getData()));
            String taskId = createData.getString("task_id");
            log.info("创建任务成功，taskId：{}", taskId);

            // 3. 轮询获取结果
            return pollResult(openCaller, taskId);
        }
    }

    private JSONObject uploadSmallFile(FileCaller fileCaller, File audioFile, FileInputStream fis) throws IOException {
        FileResp<FileResp.UploadData> uploadResp = fileCaller.fileUpload(FileReq.Upload.builder()
                .appId(appId)
                .fileName(audioFile.getName())
                .requestId(String.valueOf(System.currentTimeMillis()))
                .data(IOUtils.toByteArray(fis))
                .build());

        log.debug("小文件上传响应：{}", uploadResp);
        return JSON.parseObject(JSON.toJSONString(uploadResp.getData()));
    }

    private JSONObject uploadLargeFile(FileCaller fileCaller, File audioFile, FileInputStream fis) throws IOException {
        // 初始化分块上传
        FileResp<FileResp.InitData> initResp = fileCaller.fileInit(FileReq.Init.builder()
                .requestId(String.valueOf(System.currentTimeMillis()))
                .appId(appId)
                .build());

        JSONObject initData = JSON.parseObject(JSON.toJSONString(initResp.getData()));
        String uploadId = initData.getString("upload_id");
        log.debug("初始化分块上传，uploadId：{}", uploadId);

        // 分块上传
        int len;
        byte[] slice = new byte[SLICE_SIZE];
        int sliceId = 1;
        while ((len = fis.read(slice)) > 0) {
            if (fis.available() == 0) {
                slice = Arrays.copyOfRange(slice, 0, len);
            }
            FileResp<Void> partResp = fileCaller.filePartUpload(FileReq.PartUpload.builder()
                    .requestId(String.valueOf(System.currentTimeMillis()))
                    .appId(appId)
                    .uploadId(uploadId)
                    .sliceId(sliceId)
                    .data(slice)
                    .build());
            log.debug("分块上传成功，sliceId：{}，响应：{}", sliceId, partResp);
            sliceId++;
        }

        // 完成上传
        FileResp<Void> completeResp = fileCaller.fileUploadComplete(FileReq.Complete.builder()
                .appId(appId)
                .requestId(String.valueOf(System.currentTimeMillis()))
                .uploadId(uploadId)
                .build());

        log.debug("分块上传完成响应：{}", completeResp);
        return JSON.parseObject(JSON.toJSONString(completeResp.getData()));
    }

    private String pollResult(OpenCaller openCaller, String taskId) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME) {
            OpenResp queryResp = openCaller.query(OpenReq.Query.builder()
                    .common(OpenReq.Common.builder().appId(appId).build())
                    .business(OpenReq.QueryBusiness.builder().taskId(taskId).build())
                    .build());

            JSONObject queryData = JSON.parseObject(JSON.toJSONString(queryResp.getData()));
            String status = queryData.getString("task_status");

            if ("5".equals(status)) {
                throw new RuntimeException("转写任务已取消");
            } else if ("3".equals(status) || "4".equals(status)) {
                return parseResult(queryData);
            }

            log.info("任务处理中...");
            Thread.sleep(QUERY_INTERVAL);
        }

        throw new RuntimeException("转写超时，已等待" + (MAX_WAIT_TIME / 1000) + "秒");
    }

    private String parseResult(JSONObject queryData) {
        StringBuilder result = new StringBuilder();
        JsonParse jsonParse = gson.fromJson(queryData.toJSONString(), JsonParse.class);
        
        if (jsonParse.result != null && jsonParse.result.lattice != null) {
            for (Lattice lattice : jsonParse.result.lattice) {
                if (lattice.json_1best != null && lattice.json_1best.st != null) {
                    for (Rt rt : lattice.json_1best.st.rt) {
                        for (Ws ws : rt.ws) {
                            for (Cw cw : ws.cw) {
                                result.append(cw.w);
                            }
                        }
                    }
                }
            }
        }
        
        return result.toString();
    }

    @lombok.Data
    private static class JsonParse {
        String task_id;
        String task_status;
        String task_type;
        String force_refresh;
        Result result;
    }

    @lombok.Data
    private static class Result {
        List<Lattice> lattice;
    }

    @lombok.Data
    private static class Lattice {
        Json_1best json_1best;
    }

    @lombok.Data
    private static class Json_1best {
        St st;
    }

    @lombok.Data
    private static class St {
        List<Rt> rt;
        String rl;
    }

    @lombok.Data
    private static class Rt {
        List<Ws> ws;
    }

    @lombok.Data
    private static class Ws {
        List<Cw> cw;
    }

    @lombok.Data
    private static class Cw {
        String w;
    }
} 