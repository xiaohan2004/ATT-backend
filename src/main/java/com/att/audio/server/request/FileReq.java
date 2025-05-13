package com.att.audio.server.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Builder;
import lombok.Data;

public interface FileReq {
    @Data
    @Builder
    class Upload {
        private String requestId;//请求唯一标识
        private String appId;//应用唯一标识
        private String cloudId;//云唯一标识(0代表公有云)
        private String fileName;//文件名称
        private byte[] data;//文件上传数据
    }

    @Data
    @Builder
    class Init {
        @JSONField(name = "request_id")
        private String requestId;//请求唯一标识
        @JSONField(name = "app_id")
        private String appId;//应用唯一标识
        @JSONField(name = "cloud_id")
        private String cloudId;//云唯一标识(0代表公有云)
    }

    @Data
    @Builder
    class PartUpload {
        private String requestId;//请求唯一标识
        private String appId;//应用唯一标识
        private String uploadId;//上传唯一标识
        private int sliceId;
        private byte[] data;
    }

    @Data
    @Builder
    class Cancel {
        @JSONField(name = "request_id")
        private String requestId;//请求唯一标识
        @JSONField(name = "app_id")
        private String appId;//应用唯一标识
        @JSONField(name = "upload_id")
        private String uploadId;//上传唯一标识
    }

    @Data
    @Builder
    class Complete {
        @JSONField(name = "request_id")
        private String requestId;//请求唯一标识
        @JSONField(name = "app_id")
        private String appId;//应用唯一标识
        @JSONField(name = "upload_id")
        private String uploadId;//上传唯一标识
    }
}
