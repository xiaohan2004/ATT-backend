import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP服务器 - 接收PCM数据并生成WAV文件
 * 监听18081端口，接收开发板发送的音频数据
 */
public class AudioTCPServer {
    private static final int PORT = 18081;
    private static final String SAVE_DIR = "received_audio";
    private static final int THREAD_POOL_SIZE = 10;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static boolean isRunning = true;
    
    // WAV音频参数
    private static final int CHANNELS = 1;        // 单声道
    private static final int SAMPLE_SIZE = 2;     // 16位 = 2字节
    private static final int SAMPLE_RATE = 16000; // 16kHz采样率
    
    // 文件命名计数器
    private static int fileCounter = 0;
    private static final String FILE_PREFIX = "audio";

    public static void main(String[] args) {
        // 解析命令行参数，可选
        int bytesToRead = 32000; // 默认值，如果未指定
        if (args.length > 0) {
            try {
                bytesToRead = Integer.parseInt(args[0]);
                System.out.println("设置每个音频文件读取字节数: " + bytesToRead);
            } catch (NumberFormatException e) {
                System.err.println("无效的字节数参数，使用默认值: " + bytesToRead);
            }
        } else {
            System.out.println("未指定字节数，使用默认值: " + bytesToRead);
        }
        
        // 确保保存目录存在
        createSaveDirectory();
        
        // 初始化文件计数器
        initializeFileCounter();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false;
            threadPool.shutdown();
            System.out.println("服务器关闭中...");
        }));
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("音频接收服务器启动，监听端口: " + PORT);
            System.out.println("等待客户端连接并发送START命令...");
            System.out.println("每次接收 " + bytesToRead + " 字节数据，生成WAV文件");
            System.out.println("按Ctrl+C停止服务器");
            
            while (isRunning) {
                try {
                    // 设置接受超时，允许服务器定期检查isRunning标志
                    serverSocket.setSoTimeout(1000);
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setKeepAlive(true);
                    
                    System.out.println("\n接收到新连接: " + clientSocket.getInetAddress().getHostAddress());
                    
                    // 使用线程池处理客户端，传递bytesToRead参数
                    final int bytesToReadFinal = bytesToRead;
                    threadPool.submit(() -> handleClient(clientSocket, bytesToReadFinal));
                } catch (SocketTimeoutException e) {
                    // 超时异常，继续循环
                    continue;
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("处理客户端连接时出错: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            threadPool.shutdownNow();
        }
    }
    
    private static void handleClient(Socket clientSocket, int bytesToRead) {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        System.out.println("处理来自 " + clientAddress + " 的连接，等待START命令");
        
        try (InputStream inputStream = clientSocket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            String line;
            while (isRunning) {
                try {
                    // 检查是否有START命令
                    line = readLineWithTimeout(reader);
                    
                    if (line == null) {
                        // 连接可能已关闭
                        System.out.println("客户端可能已断开连接");
                        break;
                    }
                    
                    line = line.trim();
                    if ("START".equals(line)) {
                        System.out.println("接收到START命令，准备读取PCM数据");
                        
                        // 接收固定大小的PCM数据
                        byte[] pcmData = readExactBytes(inputStream, bytesToRead);
                        if (pcmData == null || pcmData.length < bytesToRead) {
                            System.out.println("未能接收完整数据，期望: " + bytesToRead + 
                                              " 字节, 实际接收: " + (pcmData != null ? pcmData.length : 0) + " 字节");
                            continue;
                        }
                        
                        System.out.println("已接收PCM数据: " + pcmData.length + " 字节");
                        
                        // 生成WAV文件名
                        String fileName = generateFileName();
                        Path filePath = Paths.get(SAVE_DIR, fileName);
                        
                        // 保存为WAV文件
                        saveWavFile(pcmData, filePath.toString());
                        
                        System.out.println("WAV文件已保存: " + fileName);
                        fileCounter++;
                    }
                } catch (SocketTimeoutException e) {
                    // 读取超时，继续循环
                    continue;
                } catch (IOException e) {
                    System.err.println("读取客户端数据时出错: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("处理客户端数据时出错: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("客户端连接已关闭: " + clientAddress);
            } catch (IOException e) {
                System.err.println("关闭socket时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 读取确切大小的字节数或返回null
     */
    private static byte[] readExactBytes(InputStream stream, int bytesToRead) throws IOException {
        byte[] buffer = new byte[bytesToRead];
        int totalBytesRead = 0;
        int bytesRead;
        
        while (totalBytesRead < bytesToRead) {
            bytesRead = stream.read(buffer, totalBytesRead, bytesToRead - totalBytesRead);
            if (bytesRead == -1) {
                // 连接已关闭且未读取全部数据
                if (totalBytesRead == 0) {
                    return null;
                }
                return Arrays.copyOf(buffer, totalBytesRead);
            }
            totalBytesRead += bytesRead;
        }
        
        return buffer;
    }
    
    /**
     * 带超时的行读取
     */
    private static String readLineWithTimeout(BufferedReader reader) throws IOException {
        // 设置超时
        if (reader.ready()) {
            return reader.readLine();
        }
        
        // 如果当前没有数据可读，尝试等待一小段时间
        long endTime = System.currentTimeMillis() + 500; // 500ms超时
        while (System.currentTimeMillis() < endTime) {
            if (reader.ready()) {
                return reader.readLine();
            }
            try {
                // 小睡避免CPU高负载
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("读取被中断");
            }
        }
        
        // 超时但连接可能仍然有效
        return null;
    }
    
    /**
     * 生成WAV文件名
     */
    private static String generateFileName() {
        return String.format("%s.%03d.wav", FILE_PREFIX, fileCounter);
    }
    
    /**
     * 初始化文件计数器
     */
    private static void initializeFileCounter() {
        File directory = new File(SAVE_DIR);
        File[] files = directory.listFiles((dir, name) -> 
            name.startsWith(FILE_PREFIX) && name.endsWith(".wav"));
        
        if (files != null && files.length > 0) {
            Pattern pattern = Pattern.compile(FILE_PREFIX + "\\.(\\d{3})\\.wav");
            int maxCounter = 0;
            
            for (File file : files) {
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.matches()) {
                    try {
                        int fileCount = Integer.parseInt(matcher.group(1));
                        if (fileCount > maxCounter) {
                            maxCounter = fileCount;
                        }
                    } catch (NumberFormatException e) {
                        // 忽略格式错误的文件名
                    }
                }
            }
            
            fileCounter = maxCounter + 1;
        } else {
            fileCounter = 0;
        }
        
        System.out.println("文件计数器从 " + fileCounter + " 开始");
    }
    
    /**
     * 将PCM数据保存为WAV文件
     */
    private static void saveWavFile(byte[] pcmData, String filePath) throws IOException {
        // WAV文件头长度为44字节
        int headerSize = 44;
        int dataSize = pcmData.length;
        int fileSize = headerSize + dataSize;
        
        try (FileOutputStream fos = new FileOutputStream(filePath);
             DataOutputStream dos = new DataOutputStream(fos)) {
            
            // RIFF头
            dos.writeBytes("RIFF");                                 // ChunkID
            dos.write(intToLittleEndian(fileSize - 8), 0, 4);       // ChunkSize
            dos.writeBytes("WAVE");                                 // Format
            
            // fmt子块
            dos.writeBytes("fmt ");                                 // Subchunk1ID
            dos.write(intToLittleEndian(16), 0, 4);                 // Subchunk1Size (16 for PCM)
            dos.write(shortToLittleEndian((short)1), 0, 2);         // AudioFormat (1 for PCM)
            dos.write(shortToLittleEndian((short)CHANNELS), 0, 2);  // NumChannels
            dos.write(intToLittleEndian(SAMPLE_RATE), 0, 4);        // SampleRate
            
            int byteRate = SAMPLE_RATE * CHANNELS * SAMPLE_SIZE;
            dos.write(intToLittleEndian(byteRate), 0, 4);           // ByteRate
            
            short blockAlign = (short)(CHANNELS * SAMPLE_SIZE);
            dos.write(shortToLittleEndian(blockAlign), 0, 2);       // BlockAlign
            
            short bitsPerSample = (short)(SAMPLE_SIZE * 8);
            dos.write(shortToLittleEndian(bitsPerSample), 0, 2);    // BitsPerSample
            
            // data子块
            dos.writeBytes("data");                                 // Subchunk2ID
            dos.write(intToLittleEndian(dataSize), 0, 4);           // Subchunk2Size
            
            // 写入PCM数据
            dos.write(pcmData, 0, pcmData.length);
        }
    }
    
    /**
     * 将int转换为小端字节数组
     */
    private static byte[] intToLittleEndian(int value) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte)(value & 0xFF);
        bytes[1] = (byte)((value >> 8) & 0xFF);
        bytes[2] = (byte)((value >> 16) & 0xFF);
        bytes[3] = (byte)((value >> 24) & 0xFF);
        return bytes;
    }
    
    /**
     * 将short转换为小端字节数组
     */
    private static byte[] shortToLittleEndian(short value) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte)(value & 0xFF);
        bytes[1] = (byte)((value >> 8) & 0xFF);
        return bytes;
    }
    
    private static void createSaveDirectory() {
        File directory = new File(SAVE_DIR);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("创建目录: " + SAVE_DIR);
            } else {
                System.err.println("无法创建目录: " + SAVE_DIR);
            }
        }
    }
} 