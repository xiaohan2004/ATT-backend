package com.att.audio.server.util;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class OpenResp<T> {
    private int code;
    private String sid;
    private T data;
    private String message;

    @Data
    public static class CreateData {
        @JSONField(name = "task_id")
        private String taskId;
    }
    @Data
    public static class CancelData {
        @JSONField(name = "sid")
        private String sid;
        @JSONField(name = "code")
        private Integer code;
        @JSONField(name = "message")
        private String message;
    }

    @Data
    public static class QueryData {
        @JSONField(name = "task_id")
        private String taskId;
        @JSONField(name = "task_status")
        private Integer taskStatus;
        @JSONField(name = "force_refresh")
        private Integer forceRefresh;
        @JSONField(name = "calculation_cost_time")
        private String calculationCostTime;
        @JSONField(name = "file_length")
        private Integer fileLength;
        private Object[] lattice;
        private Object[] lattice2;
    }
}
