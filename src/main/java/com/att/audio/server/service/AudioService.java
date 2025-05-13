package com.att.audio.server.service;

import java.io.IOException;

public interface AudioService {
    /**
     * 处理接收到的音频数据
     * @param audioData PCM音频数据
     * @throws IOException 如果保存文件时发生错误
     */
    void handleAudioData(byte[] audioData) throws IOException;
} 