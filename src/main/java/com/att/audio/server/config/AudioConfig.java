package com.att.audio.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "audio")
public class AudioConfig {
    private String savePath;
    private int chunkSize;
    private int sampleRate;
    private int bitsPerSample;
    private int channels;
} 