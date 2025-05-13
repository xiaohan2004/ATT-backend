package com.att.audio.server.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Builder;

public interface OpenReq {
    @lombok.Data
    @Builder
    class Create {
        private Common common;
        private Business business;
        private Data data;
    }

    @lombok.Data
    @Builder
    class Query {
        private Common common;
        private QueryBusiness business;
    }
    @lombok.Data
    @Builder
    class Cancel {
        private Common common;
        private CancelBusiness business;
    }
    @lombok.Data
    @Builder
    class CancelBusiness {
        @JSONField(name = "task_id")
        private String taskId;
        @JSONField(name = "request_id")
        private String request_id;
    }

    @lombok.Data
    @Builder
    class QueryBusiness {
        @JSONField(name = "task_id")
        private String taskId;
        @JSONField(name = "procinfo_on")
        private Integer procinfoOn;
    }

    @lombok.Data
    @Builder
    class Common {
        @JSONField(name = "app_id")
        private String appId;
    }

    @lombok.Data
    @Builder
    class Business {
        @JSONField(name = "request_id")
        private String requestId;
        private String language;
        private String accent;
        private String domain;
        @JSONField(name = "callback_url")
        private String callbackUrl;
        @JSONField(name = "callback_key")
        private String callbackKey;
        @JSONField(name = "callback_secret")
        private String callbackSecret;
        @JSONField(name = "feature_list")
        private String[] feature_list;
        @JSONField(name = "procinfo_on")
        private Integer procinfoOn;
        @JSONField(name = "speaker_num")
        private Integer speakerNum;
        @JSONField(name = "vspp_on")
        private Integer vsppOn;
        @JSONField(name = "output_type")
        private Integer outputType;
        @JSONField(name = "postproc_on")
        private Integer postprocOn;
        private String pd;
        @JSONField(name = "res_id")
        private String resId;
        private Integer duration;
        @JSONField(name = "res_url")
        private String resUrl;
        @JSONField(name = "enable_subtitle")
        private Integer enableSubtitle;
        @JSONField(name = "seg_max")
        private Integer segMax;
        @JSONField(name = "seg_min")
        private Integer segMin;
        @JSONField(name = "seg_weight")
        private Integer segWeight;
        @JSONField(name = "smoothproc")
        private Boolean smoothproc;
        @JSONField(name = "colloqproc")
        private Boolean colloqproc;
        @JSONField(name = "language_type")
        private Integer languageType;
        @JSONField(name = "vto")
        private Integer vto;
        @JSONField(name = "vad_mdn")
        private Integer vadMdn;
        @JSONField(name = "vad_margin")
        private Integer vadMargin;
        @JSONField(name = "dhw")
        private String dhw;
        private Personalization personalization;
    }

    @lombok.Data
    @Builder
    class Data {
        @JSONField(name = "audio_url")
        private String audioUrl;
        @JSONField(name = "audio_src")
        private String audioSrc;
        private String format;
        private String encoding;
    }

    enum Personalization {
        PERSONAL, WFST, LM
    }
}
