package com.att.audio.server.service.impl;

import com.att.audio.server.config.AudioConfig;
import com.att.audio.server.service.AudioService;
import com.att.audio.server.service.SpeechToTextService;
import com.att.audio.server.util.WavUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AudioServiceImpl implements AudioService {
    private final AudioConfig audioConfig;
    private final SpeechToTextService speechToTextService;
    private final RedisTemplate<String, String> redisTemplate;
    private final List<byte[]> buffer = new ArrayList<>();
    private int currentReads = 0;
    private int fileCount;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    @Autowired
    public AudioServiceImpl(AudioConfig audioConfig,
                            SpeechToTextService speechToTextService,
                            RedisTemplate<String, String> redisTemplate) {
        this.audioConfig = audioConfig;
        this.speechToTextService = speechToTextService;
        this.redisTemplate = redisTemplate;
        // 确保保存目录存在
        File saveDir = new File(audioConfig.getSavePath());
        saveDir.mkdirs();
        // 初始化文件序号
        initializeFileCount();
    }

    private void initializeFileCount() {
        File saveDir = new File(audioConfig.getSavePath());
        File[] files = saveDir.listFiles((dir, name) -> name.matches("audio_\\d{8}_\\d{6}_\\d{3}\\.wav"));
        if (files == null || files.length == 0) {
            fileCount = 1;
            return;
        }

        int maxCount = 1;
        for (File file : files) {
            String name = file.getName();
            try {
                // 从文件名中提取序号
                String countStr = name.substring(name.lastIndexOf('_') + 1, name.lastIndexOf('.'));
                int count = Integer.parseInt(countStr);
                maxCount = Math.max(maxCount, count);
            } catch (Exception e) {
                log.warn("无法从文件名解析序号: {}", name);
            }
        }
        fileCount = maxCount + 1;
        log.info("初始化文件序号: {}", fileCount);
    }

    @Override
    public synchronized void handleAudioData(byte[] audioData) throws IOException {
        // 将数据添加到缓冲区
        buffer.add(audioData);
        currentReads++;

        // 检查是否累积了足够的数据
        if (currentReads >= audioConfig.getChunkSize()) {
            // 计算总数据大小
            int totalSize = buffer.stream().mapToInt(data -> data.length).sum();

            // 合并所有数据
            byte[] combinedData = new byte[totalSize];
            int offset = 0;
            for (byte[] data : buffer) {
                System.arraycopy(data, 0, combinedData, offset, data.length);
                offset += data.length;
            }

            // 生成带时间戳的WAV文件名
            String timestamp = dateFormat.format(new Date());
            String filename = String.format("%s/audio_%s_%03d.wav",
                    audioConfig.getSavePath(), timestamp, fileCount);

            // 保存WAV文件
            saveWavFile(filename, combinedData);

            // 异步处理语音转写
            CompletableFuture.runAsync(() -> {
                try {
                    File audioFile = new File(filename);
                    String text = speechToTextService.convertToText(audioFile);
                    log.info("语音转写完成，文件：{}，结果：{}", filename, text);
                    try {
                        // 将转写结果保存到Redis
                        redisTemplate.opsForValue().set(filename, text);
                        log.info("转写结果已保存到Redis，文件：{}", filename);
                    } catch (Exception redisException) {
                        log.error("保存转写结果到Redis失败，文件：{}", filename, redisException);
                    }
                } catch (Exception sttException) {
                    log.error("语音转写失败，文件：{}", filename, sttException);
                }
            });

            // 重置缓冲区和计数器
            buffer.clear();
            currentReads = 0;
            fileCount++;

            log.info("WAV文件已保存: {}", filename);
        }
    }

    private void saveWavFile(String filename, byte[] pcmData) throws IOException {
        // 创建WAV文件头
        byte[] header = WavUtil.createWavHeader(
                pcmData.length,
                audioConfig.getSampleRate(),
                audioConfig.getBitsPerSample(),
                audioConfig.getChannels()
        );

        // 写入文件
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(header);
            fos.write(pcmData);
        }
    }
} 