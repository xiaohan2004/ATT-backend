package com.att.audio.server.util;

/**
 * WAV文件工具类
 */
public class WavUtil {
    /**
     * 创建WAV文件头
     * @param dataLength PCM数据长度
     * @param sampleRate 采样率
     * @param bitsPerSample 采样位数
     * @param channels 声道数
     * @return WAV文件头字节数组
     */
    public static byte[] createWavHeader(int dataLength, int sampleRate, 
            int bitsPerSample, int channels) {
        byte[] header = new byte[44];
        
        // RIFF header
        writeString(header, 0, "RIFF"); // ChunkID
        writeInt(header, 4, dataLength + 36); // ChunkSize
        writeString(header, 8, "WAVE"); // Format
        
        // fmt sub-chunk
        writeString(header, 12, "fmt "); // Subchunk1ID
        writeInt(header, 16, 16); // Subchunk1Size (16 for PCM)
        writeShort(header, 20, (short) 1); // AudioFormat (PCM)
        writeShort(header, 22, (short) channels); // NumChannels
        writeInt(header, 24, sampleRate); // SampleRate
        writeInt(header, 28, sampleRate * channels * bitsPerSample / 8); // ByteRate
        writeShort(header, 32, (short) (channels * bitsPerSample / 8)); // BlockAlign
        writeShort(header, 34, (short) bitsPerSample); // BitsPerSample
        
        // data sub-chunk
        writeString(header, 36, "data"); // Subchunk2ID
        writeInt(header, 40, dataLength); // Subchunk2Size
        
        return header;
    }
    
    private static void writeString(byte[] buffer, int offset, String value) {
        for (int i = 0; i < value.length(); i++) {
            buffer[offset + i] = (byte) value.charAt(i);
        }
    }
    
    private static void writeInt(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }
    
    private static void writeShort(byte[] buffer, int offset, short value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
} 