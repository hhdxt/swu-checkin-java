# 1. 基础镜像：官方 Playwright Java 镜像，自带所有浏览器依赖
FROM mcr.microsoft.com/playwright/java:v1.42.0-jammy

# 2. 设置工作目录
WORKDIR /app

# 3. 设置时区（非常重要，否则定时任务时间会对不上）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 4. 安装 Python3、相关系统依赖以及正式版 Google Chrome
RUN apt-get update && \
    # 安装基础依赖
    apt-get install -y wget python3 python3-pip libgl1 libglib2.0-0 xvfb && \
    # 直接下载正式版 Google Chrome 的安装包 (针对 Ubuntu/Debian)
    wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    # 使用 apt 安装本地包，它会自动处理所有缺失的依赖
    apt-get install -y ./google-chrome-stable_current_amd64.deb && \
    # 安装完后删掉安装包，节省空间
    rm google-chrome-stable_current_amd64.deb && \
    rm -rf /var/lib/apt/lists/*

# 5. 安装验证码识别库
# 注意：如果构建报错提示 externally-managed-environment，再加 --break-system-packages
RUN pip3 install ddddocr

# 6. 拷贝文件
# 拷贝打好的 Jar 包
COPY target/*.jar /app/app.jar
# 拷贝识别验证码的 Python 脚本
COPY src/main/java/cn/hhdxt/dingcheckinjava/utils/ocr_tool.py /app/ocr_tool.py

# 7. 终极启动指令（核心修复点）
# 不要使用 ["executable", "param"] 这种 JSON 格式，因为它对引号解析很死板
# 直接使用 Shell 格式，xvfb-run 才能正确将引号内的内容作为 -s 的值
COPY entrypoint.sh /app/entrypoint.sh

# 关键：强制转码并给权限
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
