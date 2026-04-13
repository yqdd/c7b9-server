# 属七降九服务端
属七降九 是一个基于 Spring Boot 的多功能服务端项目，集成了MIDI处理、AI对话调用、音乐信息匹配等功能。
客户端详见：<https://github.com/yqdd/c7b9-app>

---

### 快速定位核心代码：
- 续写模型文件详见：
  - <https://github.com/yqdd/c7b9-server/releases/tag/model>
- 匹配算法详见：
  - <https://github.com/yqdd/c7b9-server/blob/master/midi/src/main/java/com/ow0b/midi/library/MidiSWLibrary.java>
- 主要对话api详见：
  - <https://github.com/yqdd/c7b9-server/blob/master/src/main/java/com/ow0b/c7b9/controller/impl/chat/ChatControllerImpl.java>
- 提示词详见：
  - <https://github.com/yqdd/c7b9-server/blob/master/src/main/java/com/ow0b/c7b9/controller/impl/chat/C7b9Agent.java>
- AI调用工具类详见：
  - <https://github.com/yqdd/c7b9-server/tree/master/src/main/java/com/ow0b/ai/client>

---

### 运行演示
<https://github.com/yqdd/c7b9-server/releases/tag/video>

---

### 编译运行代码
> 由于之前硬编码了一堆apikey和服务器ip密码，当前代码无法直接编译，需要补充以下文件和代码

### · `src/main/resources/application.yml`

在 `src/main/resources/` 目录下创建 `application.yml`，并填写以下内容（**需修改用户名、密码、apikey**）：

```yaml
spring:
  mvc:
    async:
      request-timeout: -1
  application:
    name: c7b9-server
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/your_database   # 需修改
    username: your_username   # 需修改
    password: your_password   # 需修改
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8080
  ssl:
    key-store: classpath:your_certificate.jks   # 需修改
    key-store-password: your_jks_password   # 需修改
    keyStoreType: JKS

converter: http://127.0.0.1:5000   # converter api 地址
midi-library: true

ai:
  chat:
    api:
      url: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
      key: sk-xxxxxxxxxxxxxxxxxxxxxxxx   # 需修改
      model: qwen3-max
  dashscope:
    api:
      url: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
      key: sk-xxxxxxxxxxxxxxxxxxxxxxxx   # 需修改
      model: qwen3-omni-flash
  rwkv:
    api:
      url: http://127.0.0.1:8000/v1/completions   # rwkv 服务地址
      model: rwkv
```

### · `src/main/java/com/ow0b/c7b9/C7b9ServerApplication.java`

在第 78 行附近，找到并填写**端口转发服务器**的 IP 与密码：

```java
String ip = "你的ip", password = "你的密码";
```

### · SSL 证书文件

将你的 JKS 证书文件放置于 `src/main/resources/` 目录下，并确保文件名与 `application.yml` 中的 `key-store` 配置一致。

### · MIDI 文件库

在项目根目录创建 `midis` 文件夹，并将 MIDI 文件复制进去。推荐使用 [GiantMIDI-Piano](https://github.com/bytedance/GiantMIDI-Piano) 数据集中的文件。

### · 续写模型

将 [C7B9-v0.0.1.zip](https://github.com/yqdd/c7b9-server/releases/tag/model) 解压后放在converter目录下

---

补充完文件后可直接在 IDE 中运行 `C7b9ServerApplication` 主类。

### 注意事项
- 还需要运行converter目录下的app.py
- 老版本需要运行RWKV（当前版本已不需要）

---

（而且因为同样的原因git记录没有push，见下图）
> ![](img1.png)  
> ![](img2.png)