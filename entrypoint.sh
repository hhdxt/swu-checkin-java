#!/bin/bash
# 1. 打印调试信息
echo "--- [DEBUG] 脚本开始执行 ---"

# 2. 清理可能残余的锁文件
rm -f /tmp/.X99-lock

# 3. 手动在后台启动 Xvfb (指定显示器编号为 :99)
echo "--- [DEBUG] 正在启动 Xvfb 虚拟屏幕... ---"
Xvfb :99 -ac -screen 0 1280x720x24 > /dev/null 2>&1 &

# 4. 等待 X 服务器真正就绪 (最多等 10 秒)
echo "--- [DEBUG] 等待 Xvfb 就绪... ---"
for i in {1..10}; do
    if [ -e /tmp/.X11-unix/X99 ]; then
        echo "--- [DEBUG] Xvfb 已成功启动 (DISPLAY=:99) ---"
        break
    fi
    echo "--- [DEBUG] 第 $i 次尝试..."
    sleep 1
done

# 5. 导出环境变量，告诉 Playwright 去哪里找屏幕
export DISPLAY=:99

# 6. 启动 Java 程序 (增加 -Xmx 限制防止内存瞬间撑爆)
echo "--- [DEBUG] 正在启动 Java 进程... ---"
# 这里一定要用 exec，让 Java 替换当前 Shell
exec java -Xmx512m -Dfile.encoding=UTF-8 -jar /app/app.jar