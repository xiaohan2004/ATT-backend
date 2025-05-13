package com.att.audio.server.service;

import java.io.File;

public interface SpeechToTextService {
    /**
     * 将音频文件转换为文本
     * @param audioFile 音频文件
     * @return 转写结果
     */
    String convertToText(File audioFile);
    /**
     * 获取最新的转写结果
     * @return 转写结果
     */
    String getLatestTranscription();
} 