import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Arrays;

/**
 * TCP客户端 - 模拟开发板发送音频数据
 * 发送START命令和PCM音频数据
 */
public class AudioTCPClient {
    private static final int PORT = 18081;
    private static final String DEFAULT_SERVER = "localhost";
    private static final int BUFFER_SIZE = 8192;
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法: java AudioTCPClient <WAV/PCM文件路径> [服务器地址] [重复次数]");
            System.out.println("示例: java AudioTCPClient sample.wav");
            System.out.println("      java AudioTCPClient sample.pcm 192.168.1.100 5");
            return;
        }
        
        String filePath = args[0];
        String serverAddress = (args.length > 1) ? args[1] : DEFAULT_SERVER;
        int repeatCount = (args.length > 2) ? Integer.parseInt(args[2]) : 1;
        
        File audioFile = new File(filePath);
        if (!audioFile.exists() || !audioFile.isFile()) {
            System.err.println("错误: 无法找到文件 " + filePath);
            return;
        }
        
        // 读取音频数据
        byte[] audioData;
        try {
            audioData = readAudioFile(filePath);
        } catch (IOException e) {
            System.err.println("读取音频文件时出错: " + e.getMessage());
            return;
        }
        
        System.out.println("已读取音频数据: " + audioData.length + " 字节");
        System.out.println("将发送到服务器 " + serverAddress + ":" + PORT);
        System.out.println("重复次数: " + repeatCount);
        
        // 连接服务器并发送数据
        try (Socket socket = new Socket(serverAddress, PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             OutputStream outputStream = socket.getOutputStream()) {
            
            System.out.println("已连接到服务器 " + serverAddress + ":" + PORT);
            
            for (int i = 0; i < repeatCount; i++) {
                System.out.println("\n发送第 " + (i+1) + " 次音频数据...");
                
                // 发送START命令
                writer.println("START");
                writer.flush();
                System.out.println("已发送START命令");
                
                // 发送音频数据
                outputStream.write(audioData);
                outputStream.flush();
                System.out.println("已发送音频数据: " + audioData.length + " 字节");
                
                // 等待一段时间再发送下一次
                if (i < repeatCount - 1) {
                    System.out.println("等待1秒...");
                    Thread.sleep(1000);
                }
            }
            
            System.out.println("\n所有数据发送完成! 共 " + repeatCount + " 次");
            
        } catch (UnknownHostException e) {
            System.err.println("错误: 无法找到服务器 " + serverAddress);
        } catch (ConnectException e) {
            System.err.println("错误: 无法连接到服务器 " + serverAddress + ":" + PORT);
            System.err.println("请确保服务器已启动并且端口已打开");
        } catch (IOException e) {
            System.err.println("发送数据时出错: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("线程被中断: " + e.getMessage());
        }
    }
    
    /**
     * 读取音频文件内容
     * 如果是WAV文件，跳过WAV文件头，只读取PCM数据
     */
    private static byte[] readAudioFile(String filePath) throws IOException {
        boolean isWavFile = filePath.toLowerCase().endsWith(".wav");
        
        try (InputStream inputStream = new FileInputStream(filePath)) {
            // 如果是WAV文件，跳过44字节的头部
            if (isWavFile) {
                System.out.println("检测到WAV文件，将跳过WAV头部...");
                byte[] header = new byte[44];
                int headerBytesRead = inputStream.read(header);
                if (headerBytesRead < 44) {
                    throw new IOException("WAV文件头不完整");
                }
                
                // 提取data块大小
                int dataSize = ((header[43] & 0xFF) << 24) | 
                              ((header[42] & 0xFF) << 16) | 
                              ((header[41] & 0xFF) << 8) | 
                              (header[40] & 0xFF);
                
                System.out.println("WAV文件头中的数据大小: " + dataSize + " 字节");
                
                byte[] pcmData = new byte[dataSize];
                int totalRead = 0;
                int bytesRead;
                
                while (totalRead < dataSize && 
                       (bytesRead = inputStream.read(pcmData, totalRead, dataSize - totalRead)) != -1) {
                    totalRead += bytesRead;
                }
                
                if (totalRead < dataSize) {
                    System.out.println("警告: 只读取了 " + totalRead + " 字节，少于预期的 " + dataSize + " 字节");
                    return Arrays.copyOf(pcmData, totalRead);
                }
                
                return pcmData;
            } else {
                // 如果是PCM文件，直接读取所有数据
                return inputStream.readAllBytes();
            }
        }
    }
} 