# 西南大学自动打卡工具

基于 Java 的西南大学宿舍打卡自动化的定时任务工具。

## 功能特性

- 自动登录西南大学统一认证系统
- 自动完成健康打卡
- 定时执行任务（默认每天 21:10）
- 邮件通知打卡结果（成功/失败）
- 支持多账号配置
- 失败自动重试（最多 3 次）

## 技术栈

- Java 17
- Spring Boot 3
- Playwright（浏览器自动化）
- BillionMail邮件服务

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

### 4. 构建运行

```bash
# 构建
./mvnw clean package -DskipTests

# 运行
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
