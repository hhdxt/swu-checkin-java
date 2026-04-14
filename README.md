# SWU自动打卡工具

基于 Java 的SWU宿舍打卡自动化的定时任务工具。

## 功能特性

- 自动登录SWU统一认证系统
- 自动完成健康打卡
- 定时执行任务（默认每天 21:10）
- 邮件通知打卡结果（成功/失败）
- 支持多账号配置
- 失败自动重试（最多 3 次）

## 技术栈

- Java 17
- Spring Boot 3
- Playwright（浏览器自动化）
- Xvfb（虚拟显示器）
- XXL-Job（分布式定时任务）

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/your-username/ding-checkin-java.git
cd ding-checkin-java
```

### 2. 配置账号

编辑 `src/main/resources/application-dev.yml`：

```yaml
info:
  user-account:
    - username: 你的学号
      password: 你的密码
      email: 你的邮箱
```

### 3. 配置邮件服务（可选）

编辑 `src/main/java/cn/hhdxt/dingcheckinjava/DingCheckinJavaApplication.java` 中的 API Key：

```java
// BillionMail邮件服务 API Key
private static final String API_KEY_SUCCESS = "你的成功邮件API密钥";
private static final String API_KEY_FAILURE = "你的失败邮件API密钥";
```

### 4. 构建 Jar 包

```bash
./mvnw clean package -DskipTests
```

### 5. Docker 部署（推荐）

#### 5.1 构建镜像

```bash
docker build -t swu-checkin-xvfb:v1 .
```

#### 5.2 启动容器

```bash
docker run -d -p 8080:8080 --name swu-checkin-task \
  --restart unless-stopped \
  --memory="2g" \
  -e TZ=Asia/Shanghai \
  swu-checkin-xvfb:v1
```

参数说明：
- `-d`: 后台运行
- `-p 8080:8080`: 端口映射（宿主机端口:容器端口）
- `--name swu-checkin-task`: 容器名称
- `--restart unless-stopped`: 开机自启
- `--memory="2g"`: 限制内存使用
- `-e TZ=Asia/Shanghai`: 设置时区

#### 5.3 测试

首次运行会下载浏览器依赖包，请耐心等待。

```bash
# 测试打卡接口
curl http://localhost:8080/test

# 查看运行日志
docker logs -f swu-checkin-task
```

正常响应说明：
- `暂无打卡计划`: 程序运行正常，当前没有打卡任务
- `打卡成功`: 打卡成功
- `今日已打卡`: 今日已打过卡

#### 5.4 停止与重启

```bash
# 停止容器
docker stop swu-checkin-task

# 重启容器
docker restart swu-checkin-task
```

### 6. 本地自动打卡

将程序部署到自己的电脑上，保持电脑开机并联网，即可实现每天自动打卡。

### 7. 直接运行（非 Docker）

```bash
java -jar target/ding-checkin-java-0.0.1-SNAPSHOT.jar
```

## 配置说明

| 配置项 | 说明 |
|--------|------|
| `username` | 西南大学统一认证账号（学号） |
| `password` | 西南大学统一认证密码 |
| `email` | 接收通知的邮箱地址 |
| `server.port` | 服务端口（默认 8080） |

### 定时任务

默认执行时间：每天 21:10

如需修改，编辑 `DingCheckinJavaApplication.java`：

```java
@Scheduled(cron = "0 10 21 * * ?")  // 秒 分 时 日 月 周
```

## 项目结构

```
src/
├── main/
│   ├── java/cn/hhdxt/dingcheckinjava/
│   │   ├── config/          # 配置类
│   │   ├── controller/       # 控制器
│   │   ├── demo/             # 示例代码
│   │   ├── properties/       # 属性配置
│   │   ├── service/         # 服务层
│   │   └── utils/           # 工具类
│   └── resources/
│       └── application*.yml  # 配置文件
└── test/                    # 测试代码
```

## 注意事项

1. **账号安全**：请勿将包含真实账号密码的配置文件提交到仓库
2. **学工系统**：请确保账号有打卡权限
3. **网络环境**：需要能够访问西南大学相关系统

## 许可证

MIT License
