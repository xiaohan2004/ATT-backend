package com.att.audio.server.util;

import com.att.audio.server.request.*;
import com.alibaba.fastjson.JSON;
import lombok.Builder;
import lombok.Data;
import okhttp3.*;
import okio.Buffer;

@Data
@Builder
public class FileCaller {
    private String ulrPrefix;
    private String apiKey;
    private String apiSecret;
    private OkHttpClient client;

    public FileResp<FileResp.UploadData> fileUpload(FileReq.Upload req) {
        try {
            String reqUrl = ulrPrefix + "/upload";
            RequestBody requestBody = new MultipartBody.Builder().
                    setType(MultipartBody.FORM).
                    addFormDataPart("request_id", req.getRequestId()).
                    addFormDataPart("app_id", req.getAppId()).
                    addFormDataPart("data", req.getFileName(), RequestBody.create(MediaType.parse("application/octet-stream"), req.getData())).
                    build();

            Authentication.AUthResult result = Authentication.auth(Authentication.AuthParam.builder()
                    .apiKey(apiKey).apiSecret(apiSecret).method(Authentication.Method.POST).reqUrl(reqUrl).build());

            Request request = new Request.Builder().url(reqUrl).
                    addHeader("Content-Type", "multipart/form-data").
                    addHeader("Date", result.getDate()).
                    addHeader("Digest",result.getDigest()).
                    addHeader("Authorization", result.getAuthorization()).
                    post(requestBody).build();

            Response resp = client.newCall(request).execute();
            if (resp.code() != 200) {
                System.out.println(JSON.parseObject(resp.body().bytes(), FileResp.class).toString());
                // throw new RuntimeException(String.format("http response is not 200(%v)", resp.code()));
            }
            return JSON.parseObject(resp.body().bytes(), FileResp.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public FileResp<FileResp.InitData> fileInit(FileReq.Init req) {
        try {
            String reqUrl = ulrPrefix + "/mpupload/init";
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JSON.toJSONString(req));
            Authentication.AUthResult result = Authentication.auth(Authentication.AuthParam.builder()
                    .apiKey(apiKey).apiSecret(apiSecret).method(Authentication.Method.POST).reqUrl(reqUrl).build());
            Request request = new Request.Builder().url(reqUrl).
                    addHeader("Content-Type", "application/json").
                    addHeader("Date", result.getDate()).
                    addHeader("Digest", result.getDigest()).
                    addHeader("Authorization", result.getAuthorization()).
                    post(requestBody).build();
            Response resp = client.newCall(request).execute();
            if (resp.code() != 200) {
                System.out.println(JSON.parseObject(resp.body().bytes(), FileResp.class).toString());
                // throw new RuntimeException(String.format("http response is not 200(%v)", resp.code()));
            }
            return JSON.parseObject(resp.body().bytes(), FileResp.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public FileResp<Void> filePartUpload(FileReq.PartUpload req) {
        try {
            String reqUrl = ulrPrefix + "/mpupload/upload";
            RequestBody requestBody = new MultipartBody.Builder().
                    setType(MultipartBody.FORM).
                    addFormDataPart("request_id", req.getRequestId()).
                    addFormDataPart("app_id", req.getAppId()).
                    addFormDataPart("upload_id", req.getUploadId()).
                    addFormDataPart("slice_id", String.valueOf(req.getSliceId())).
                    addFormDataPart("data", String.valueOf(req.getSliceId()), RequestBody.create(MediaType.parse("application/octet-stream"), req.getData())).
                    build();
            Authentication.AUthResult result = Authentication.auth(Authentication.AuthParam.builder()
                    .apiKey(apiKey).apiSecret(apiSecret).method(Authentication.Method.POST).reqUrl(reqUrl).build());
            Request request = new Request.Builder().url(reqUrl).
                    addHeader("Content-Type", "multipart/form-data").
                    addHeader("Date", result.getDate()).
                    addHeader("Digest", result.getDigest()).
                    addHeader("Authorization", result.getAuthorization()).
                    post(requestBody).build();
            Response resp = client.newCall(request).execute();
            if (resp.code() != 200) {
                System.out.println(JSON.parseObject(resp.body().bytes(), FileResp.class).toString());
                // throw new RuntimeException(String.format("http response is not 200(%v)", resp.code()));
            }
            return JSON.parseObject(resp.body().bytes(), FileResp.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public FileResp<Void> fileUploadComplete(FileReq.Complete req) {
        try {
            String reqUrl = ulrPrefix + "/mpupload/complete"; // 分片上传请求的url
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JSON.toJSONString(req));
            Authentication.AUthResult result = Authentication.auth(Authentication.AuthParam.builder()
                    .apiKey(apiKey).apiSecret(apiSecret).method(Authentication.Method.POST).reqUrl(reqUrl).build());
            Request request = new Request.Builder().url(reqUrl).
                    addHeader("Content-Type", "application/json").
                    addHeader("Date", result.getDate()).
                    addHeader("Digest", result.getDigest()).
                    addHeader("Authorization", result.getAuthorization()).
                    post(requestBody).build();
            Response resp = client.newCall(request).execute();
            if (resp.code() != 200) {
                System.out.println(JSON.parseObject(resp.body().bytes(), FileResp.class).toString());
                // throw new RuntimeException(String.format("http response is not 200(%v)", resp.code()));
            }
            return JSON.parseObject(resp.body().bytes(), FileResp.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public FileResp<Void> fileCancel(FileReq.Cancel req) {
        try {
            String reqUrl = ulrPrefix + "/mpupload/cancel";
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JSON.toJSONString(req));
            Authentication.AUthResult result = Authentication.auth(Authentication.AuthParam.builder()
                    .apiKey(apiKey).apiSecret(apiSecret).method(Authentication.Method.POST).reqUrl(reqUrl).build());
            Request request = new Request.Builder().url(reqUrl).
                    addHeader("Content-Type", "application/json").
                    addHeader("Date", result.getDate()).
                    addHeader("Digest", result.getDigest()).
                    addHeader("Authorization", result.getAuthorization()).
                    post(requestBody).build();
            Response resp = client.newCall(request).execute();
            if (resp.code() != 200) {
                System.out.println(JSON.parseObject(resp.body().bytes(), FileResp.class).toString());
                // throw new RuntimeException(String.format("http response is not 200(%v)", resp.code()));
            }
            return JSON.parseObject(resp.body().bytes(), FileResp.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
