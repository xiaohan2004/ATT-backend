package com.att.audio.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "xfyun.lfasr")
public class XfyunConfig {
    private String appId;
    private String apiKey;
    private String apiSecret;
    private String host = "http://raasr.xfyun.cn/api";
    private String prepareUrl = "/prepare";
    private String uploadUrl = "/upload";
    private String mergeUrl = "/merge";
    private String getResultUrl = "/getResult";
    private String getProgressUrl = "/getProgress";
    private int sliceSize = 10485760; // 10M

    // Getters and Setters
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPrepareUrl() {
        return host + prepareUrl;
    }

    public void setPrepareUrl(String prepareUrl) {
        this.prepareUrl = prepareUrl;
    }

    public String getUploadUrl() {
        return host + uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getMergeUrl() {
        return host + mergeUrl;
    }

    public void setMergeUrl(String mergeUrl) {
        this.mergeUrl = mergeUrl;
    }

    public String getGetResultUrl() {
        return host + getResultUrl;
    }

    public void setGetResultUrl(String getResultUrl) {
        this.getResultUrl = getResultUrl;
    }

    public String getGetProgressUrl() {
        return host + getProgressUrl;
    }

    public void setGetProgressUrl(String getProgressUrl) {
        this.getProgressUrl = getProgressUrl;
    }

    public int getSliceSize() {
        return sliceSize;
    }

    public void setSliceSize(int sliceSize) {
        this.sliceSize = sliceSize;
    }
} 