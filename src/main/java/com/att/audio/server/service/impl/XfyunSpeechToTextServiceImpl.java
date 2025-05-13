package com.att.audio.server.service.impl;

import com.att.audio.server.config.XfyunConfig;
import com.att.audio.server.iflytek.XfyunSpeedTranscription;
import com.att.audio.server.service.SpeechToTextService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class XfyunSpeechToTextServiceImpl implements SpeechToTextService {
    private static final String LATEST_TRANSCRIPTION_KEY = "audio:transcription:latest";
    private static final String LATEST_TRANSCRIPTION_TIME_KEY = "audio:transcription:latest:time";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final XfyunSpeedTranscription transcription;
    private final StringRedisTemplate redisTemplate;

    public XfyunSpeechToTextServiceImpl(XfyunConfig xfyunConfig, StringRedisTemplate redisTemplate) {
        this.transcription = XfyunSpeedTranscription.builder()
                .appId(xfyunConfig.getAppId())
                .apiKey(xfyunConfig.getApiKey())
                .apiSecret(xfyunConfig.getApiSecret())
                .client(new OkHttpClient.Builder().build())
                .gson(new Gson())
                .build();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String convertToText(File audioFile) {
        try {
            String text = transcription.convertToText(audioFile);
            // 保存最新的转写结果到 Redis
            saveLatestTranscription(text);
            return text;
        } catch (Exception e) {
            log.error("语音转写失败", e);
            throw new RuntimeException("语音转写失败", e);
        }
    }

    private void saveLatestTranscription(String text) {
        try {
            String currentTime = LocalDateTime.now().format(TIME_FORMATTER);
            redisTemplate.opsForValue().set(LATEST_TRANSCRIPTION_KEY, text);
            redisTemplate.opsForValue().set(LATEST_TRANSCRIPTION_TIME_KEY, currentTime);
            log.info("已将最新转写结果保存到 Redis，时间：{}", currentTime);
        } catch (Exception e) {
            log.error("保存转写结果到 Redis 失败", e);
        }
    }

    @Override
    public String getLatestTranscription() {
        try {
            String text = redisTemplate.opsForValue().get(LATEST_TRANSCRIPTION_KEY);
            String time = redisTemplate.opsForValue().get(LATEST_TRANSCRIPTION_TIME_KEY);
            
            if (text == null || time == null) {
                return "暂无转写记录";
            }
            
            return String.format("[%s] %s", time, text);
        } catch (Exception e) {
            log.error("获取最新转写结果失败", e);
            return "获取转写记录失败";
        }
    }
} 