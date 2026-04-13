# 1. 基础镜像：官方 Playwright Java 镜像，自带所有浏览器依赖
FROM mcr.microsoft.com/playwright/java:v1.42.0-jammy

# 2. 设置工作目录
WORKDIR /app

# 3. 设置时区（非常重要，否则定时任务时间会对不上）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 4. 安装 Python3 和相关系统依赖
# ddddocr 运行需要 libgl1 (OpenCV依赖) 和 libglib2.0-0
RUN apt-get update && \
    apt-get install -y python3 python3-pip libgl1 libglib2.0-0 xvfb && \
    rm -rf /var/lib/apt/lists/*

# 5. 安装验证码识别库
# 既然提示没有那个 option，直接去掉它即可
RUN pip3 install ddddocr

# 6. 拷贝文件
# 拷贝打好的 Jar 包
COPY target/*.jar /app/app.jar
# 拷贝识别验证码的 Python 脚本
COPY src/main/java/cn/hhdxt/dingcheckinjava/utils/ocr_tool.py /app/ocr_tool.py

# 7. 终极启动指令
# xvfb-run -a 会在启动 java 之前先在内存里开启一个虚拟的图形界面环境
# -s "-screen 0 1280x720x24" 设置虚拟屏幕的分辨率和色彩深度
ENTRYPOINT ["xvfb-run", "-a", "-s", "-screen 0 1280x720x24", "java", "-jar", "/app/app.jar"]