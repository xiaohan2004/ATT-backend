package com.att.audio.server.util;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class FileResp<T> {
    private int code;
    private String sid;
    private T data;
    private String message;

    @Data
    public static class UploadData {
        private String url;
    }

    @Data
    public static class InitData {
        @JSONField(name = "upload_id")
        private String upload_id;
    }
}
