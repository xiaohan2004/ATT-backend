package com.att.audio.server.controller;

import com.att.audio.server.service.AudioService;
import com.att.audio.server.service.SpeechToTextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api")
public class AudioController {
    private final AudioService audioService;
    private final SpeechToTextService speechToTextService;

    @Autowired
    public AudioController(AudioService audioService, SpeechToTextService speechToTextService) {
        this.audioService = audioService;
        this.speechToTextService = speechToTextService;
    }

    @PostMapping
    public ResponseEntity<String> handleAudioData(@RequestBody byte[] audioData) {
        try {
            log.info("接收到音频数据: {} 字节", audioData.length);
            audioService.handleAudioData(audioData);
            return ResponseEntity.ok("{\"status\": \"success\", \"message\": \"Data received and saved\"}");
        } catch (IOException e) {
            log.error("处理音频数据时出错", e);
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"Failed to process audio data: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/latest")
    public String getLatestTranscription() {
        return speechToTextService.getLatestTranscription();
    }
} 