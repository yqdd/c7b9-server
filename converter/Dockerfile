FROM pytorch/pytorch:1.4-cuda10.1-cudnn7-runtime

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# 安装系统依赖（如果需要）
RUN apt-get update && apt-get install -y \
    && rm -rf /var/lib/apt/lists/*

# 首先复制 requirements.txt（不包含 torch）
COPY requirements.txt .

# 使用国内镜像源安装其他 Python 依赖
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple/ -r requirements.txt

# 创建模型文件目录
RUN mkdir -p /root/piano_transcription_inference_data

# 复制模型文件（确保本地有这个文件）
COPY note_F1=0.9677_pedal_F1=0.9186.pth /root/piano_transcription_inference_data/

# 复制应用代码
COPY . .

# 暴露端口
EXPOSE 5000

# 设置启动命令
CMD ["python", "app.py"]