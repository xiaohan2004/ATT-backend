const http = require('http');
const fs = require('fs');
const path = require('path');
const port = 18081;

let count = 1; // 文件计数器
let buffer = []; // 用于累积数据的缓冲区
let currentReads = 0; // 当前累积的数据块数量
const maxReads = 5; // 每个文件累积的数据块数量

// 创建 HTTP 服务器
const server = http.createServer((req, res) => {
    // 检查请求方法是否为 POST
    if (req.method === 'POST' && req.url === '/api') {
        let body = [];

        // 监听数据事件，收集请求体中的数据
        req.on('data', (chunk) => {
            body.push(chunk); // 将数据块添加到数组中
        });

        // 监听结束事件，处理完整的请求体
        req.on('end', () => {
            try {
                // 将数据块合并为一个完整的二进制缓冲区
                const data = Buffer.concat(body);

                // 将数据添加到缓冲区
                buffer.push(data);
                currentReads += 1;

                // 检查是否累积了足够的数据
                if (currentReads >= maxReads) {
                    // 合并缓冲区中的所有数据
                    const combinedData = Buffer.concat(buffer);

                    // 保存数据为 WAV 文件
                    const filename = `output${count}.wav`;
                    const filePath = path.join(__dirname, filename);

                    // 创建 WAV 文件头
                    const wavHeader = createWavHeader(combinedData.length, 16000, 16, 1);

                    // 写入文件
                    fs.writeFileSync(filePath, Buffer.concat([wavHeader, combinedData]));
                    console.log(`WAV 文件已保存: ${filePath}`);

                    // 重置缓冲区和计数器
                    buffer = [];
                    currentReads = 0;
                    count += 1;
                }

                // 发送 JSON 响应
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ status: 'success', message: 'Data received and saved as WAV file' }));
            } catch (error) {
                // 如果处理失败，返回错误响应
                console.error('处理数据失败:', error);
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Internal Server Error' }));
            }
        });
    } else {
        // 如果不是 POST 请求，返回 405 Method Not Allowed
        res.writeHead(405, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Only POST method is supported' }));
    }
});

// 启动服务器并监听指定端口
server.listen(port, () => {
    console.log(`HTTP 服务器已启动，监听端口 ${port}`);
});

// 创建 WAV 文件头
function createWavHeader(dataLength, sampleRate, bitsPerSample, channels) {
    const header = Buffer.alloc(44);

    // RIFF header
    header.write('RIFF', 0);
    header.writeUInt32LE(dataLength + 36, 4); // ChunkSize
    header.write('WAVE', 8);

    // fmt sub-chunk
    header.write('fmt ', 12);
    header.writeUInt32LE(16, 16); // Subchunk1Size
    header.writeUInt16LE(1, 20); // AudioFormat (PCM)
    header.writeUInt16LE(channels, 22); // NumChannels
    header.writeUInt32LE(sampleRate, 24); // SampleRate
    header.writeUInt32LE(sampleRate * channels * bitsPerSample / 8, 28); // ByteRate
    header.writeUInt16LE(channels * bitsPerSample / 8, 32); // BlockAlign
    header.writeUInt16LE(bitsPerSample, 34); // BitsPerSample

    // data sub-chunk
    header.write('data', 36);
    header.writeUInt32LE(dataLength, 40); // Subchunk2Size

    return header;
}
