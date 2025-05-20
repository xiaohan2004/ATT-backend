# 音频接收与语音转写服务

本项目是一个基于 Spring Boot 的后端服务，支持通过 HTTP 接收音频数据，自动调用讯飞开放平台进行语音转写，并将结果存储到 Redis，可通过接口获取最新转写内容。

## 主要功能
- 提供 HTTP 接口接收客户端上传的音频数据
- 自动保存音频文件并调用讯飞语音转写 API
- 转写结果自动存入 Redis，便于后续查询
- 提供接口获取最新的语音转写文本及时间

## 依赖环境
- Java 8+
- Maven
- Redis
- 讯飞开放平台账号（需配置 appId、apiKey、apiSecret）

## 依赖库（已在 `pom.xml` 配置）
- Spring Boot
- OkHttp3
- fastjson
- gson
- commons-io
- spring-boot-starter-data-redis

## 配置
在 `application.yml` 或 `application.properties` 中添加讯飞和 Redis 配置：

```yaml
xfyun:
  lfasr:
    app-id: 你的AppId
    api-key: 你的ApiKey
    api-secret: 你的ApiSecret
spring:
  redis:
    host: 127.0.0.1
    port: 6379
```

## 编译与启动

```bash
mvn clean package
java -jar target/你的jar包名.jar
```

## 主要接口

### 1. 上传音频数据
- **接口**：`POST /api`
- **参数**：音频二进制数据（body 直接传递字节流）
- **返回**：
  ```json
  {
    "status": "success",
    "message": "Data received and saved"
  }
  ```

### 2. 获取最新语音转写结果
- **接口**：`GET /api/latest`
- **返回**：
  字符串，格式为 `[时间]文本`
  例如：`[2024-01-18 15:30:45] 你好，这是一段语音转写内容`
- **无记录时返回**：`暂无转写记录`

## 目录结构简述
- `controller/AudioController.java` —— HTTP接口入口
- `service/AudioService.java`、`SpeechToTextService.java` —— 业务接口
- `service/impl/AudioServiceImpl.java`、`XfyunSpeechToTextServiceImpl.java` —— 业务实现
- `iflytek/XfyunSpeedTranscription.java` —— 讯飞API调用核心逻辑
- `config/XfyunConfig.java` —— 讯飞API配置
- `util/`、`request/` —— 工具类与API请求封装

## 其他说明
- 需保证 Redis 服务可用
- 讯飞 API 需开通并配置好相关参数
- 支持多客户端并发上传音频 