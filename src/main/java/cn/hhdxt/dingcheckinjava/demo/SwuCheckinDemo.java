package cn.hhdxt.dingcheckinjava.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;

@Component
public class SwuCheckinDemo {

    private static final ObjectMapper mapper = new ObjectMapper();

    // 保留你的 HttpClient，用于打卡业务
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();

    public static boolean checkin(String username, String password) {
        if (username == null || password == null) {
            System.err.println("❌ 缺少用户名或密码。");
            return false;
        }

        try {
            // 1. 使用 Playwright 自动化获取终极 Token
            System.out.println("========== [1] 开始通过浏览器获取授权 Token ==========");
            String token = getTokenWithPlaywright(username, password);
            if (token == null || token.isEmpty()) {
                System.err.println("❌ 获取 Token 失败，打卡终止！");
                return false;
            }
            System.out.println("✅ 成功拿到打卡通行证 Token: " + token);

            // 2. 拿到 Token 后，请求今日打卡任务
            System.out.println("========== [2] 开始查询今日打卡任务 ==========");
            JsonNode transitionToday = getTransitionToday(token);

            if (transitionToday == null || transitionToday.isEmpty()) {
                System.out.println("🟢 暂无打卡任务");
                return false;
            }

            if ("已签到".equals(transitionToday.path("qdzt").asText())) {
                System.out.println("🟢 今日已打卡，无需重复提交");
                return true; // 已经签到也算成功完成流程
            }

            // 3. 执行最终打卡提交
            System.out.println("========== [3] 执行最终打卡提交 ==========");
            checkin(token, transitionToday);
            System.out.println("🎉 打卡成功！");
            return true;

        } catch (Exception e) {
            System.err.println("❌ 运行过程中发生系统级异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private static void checkin(String token, JsonNode transition) throws Exception {
        // 保留之前的逻辑...
        String formId = transition.path("formId").asText();
        String id = transition.path("id").asText();
        String studentId = getStudentId(token);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("id", id);
        payload.put("formId", formId);
        payload.put("tsrq", LocalDate.now().toString());
        payload.put("xh", studentId);

        ArrayNode qdsj = payload.putArray("qdsj");
        qdsj.add("21:00").add("23:30");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://of.swu.edu.cn/gateway/fighter-baida/api/form-instance/save?formId=" + formId + "&isSubmitProcess=false"))
                .header("fighter-auth-token", token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 核心：用真实浏览器接管登录流程，并拦截最终的 Auth Token
     */
    private static String getTokenWithPlaywright(String username, String password) throws Exception {
        // ================== Spring Boot 定时任务 ClassLoader 修复 ==================
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(SwuCheckinDemo.class.getClassLoader());

        try (Playwright playwright = Playwright.create()) {
            System.out.println(">>> 正在初始化 Playwright 引擎...");
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    // 部署到服务器或后台静默运行时，建议改为 true
                    .setHeadless(false)
                    .setChannel("chrome") // 使用本机真实 Chrome
                    .setIgnoreDefaultArgs(Arrays.asList("--enable-automation")) // 仅剔除自动化启动条
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled", // 禁用底层自动化标志
                            "--window-size=1280,720"
                    ));
            Browser browser = playwright.chromium().launch(options);

            // 什么 UA 都不改，什么 JS 都不注入，完全信任本机真实的 Chrome 环境
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
            Page page = context.newPage();

            String loginUrl = "https://of.swu.edu.cn/cas/oauth/login/SWU_CAS2_FEDERAL?service=https%3A%2F%2Fof.swu.edu.cn%2Fgateway%2Ffighter-middle%2Fapi%2Fintegrate%2Fuaap%2Fcas%2Fresolve-cas-return%3Fnext%3Dhttps%253A%252F%252Fof.swu.edu.cn%252F%2523%252FcasLogin%253Ffrom%253D%25252FappCenter";
            System.out.println(">>> 开始访问登录入口...");

            // 恢复正常的等待策略
            page.navigate(loginUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(3000);

            // ================= 处理中间跳转页 =================
            try {
                System.out.println(">>> 检查是否需要点击中间页的【统一认证按钮】...");
                page.waitForSelector(".loginMethd", new Page.WaitForSelectorOptions().setTimeout(5000));
                System.out.println("👉 发现中间页按钮，正在点击...");
                page.click(".loginMethd");

                System.out.println(">>> 等待真实账号输入框加载...");
                page.waitForSelector("#loginName", new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (TimeoutError e) {
                System.out.println("⏭️ 未发现中间页按钮或超时，检查页面是否已直接进入账号密码界面...");
            }

            page.waitForTimeout(1000); // 确保验证码图片加载完毕

            boolean loginSuccess = false;
            int maxRetries = 3;

            for (int i = 1; i <= maxRetries; i++) {
                System.out.println("================ 尝试第 " + i + " 次登录 ================");
                Locator captchaLocator = page.locator("#kaptchaImage");

                if (!captchaLocator.isVisible()) {
                    System.err.println("❌ 找不到验证码图片！可能被防护拦截或页面结构发生改变");
                    throw new RuntimeException("页面未渲染出验证码");
                }

                String captchaPath = "captcha.png";
                captchaLocator.screenshot(new Locator.ScreenshotOptions().setPath(Paths.get(captchaPath)));
                System.out.println("📸 验证码截图保存成功，正在调用 Python OCR...");

                String code = recognizeCaptchaWithPython(captchaPath);
                System.out.println("🔍 OCR 识别结果: [" + code + "]");

                System.out.println("✍️ 正在填充账号、密码、验证码...");
                page.fill("#loginName", username);
                page.fill("#password", password);
                page.fill("#validateCode", code);

                page.waitForTimeout(500); // 给 JS 一点点时间处理密文
                System.out.println("🖱️ 点击登录按钮...");
                page.click("#button");

                try {
                    System.out.println(">>> 正在等待登录响应结果...");
                    // 等待直到 URL 不再是登录页
                    page.waitForCondition(() -> !page.url().contains("am/UI/Login"), new Page.WaitForConditionOptions().setTimeout(8000));
                    System.out.println("✅ URL 发生变化，判定登录表单提交成功！新 URL: " + page.url());
                    loginSuccess = true;
                    break;
                } catch (TimeoutError e) {
                    System.out.println("[WARN] ⚠️ 登录未发生跳转！当前 URL 仍然是: " + page.url());
                    System.out.println("[WARN] 🔄 准备清空密码并刷新验证码重试...");
                    page.fill("#password", "");
                    captchaLocator.click();
                    page.waitForTimeout(1500);
                }
            }

            if (!loginSuccess) {
                throw new RuntimeException("连续 " + maxRetries + " 次验证码识别或登录失败");
            }

            System.out.println(">>> 登录成功，正在紧盯 URL 等待 CAS 颁发票据 (Ticket)...");

            try {
                // 1. 死死盯住 URL，只要里面出现了 ticket=，立刻拦截！(最多等 10 秒)
                page.waitForCondition(() -> page.url().contains("ticket="), new Page.WaitForConditionOptions().setTimeout(10000));

                String currentUrl = page.url();
                System.out.println("✅ 成功捕获重定向 URL: " + currentUrl);

                // 2. 从 URL 中精准切割出 ST 票据
                String ticket = currentUrl.split("ticket=")[1].split("&")[0];
                System.out.println("🎫 成功抠出服务票据 (Service Ticket): " + ticket);

                // 3. 主动出击：利用 Playwright 当前上下文直接发 GET 请求换 Token！
                String exchangeUrl = "https://of.swu.edu.cn/gateway/fighter-middle/api/integrate/uaap/cas/exchange-token?token=" + ticket + "&remember=true";
                System.out.println("🚀 正在主动请求后端接口兑换 Token...");

                com.microsoft.playwright.APIResponse apiResponse = context.request().get(exchangeUrl);

                if (apiResponse.status() == 200) {
                    JsonNode rootNode = mapper.readTree(apiResponse.text());
                    String finalToken = rootNode.path("data").asText();

                    if (finalToken != null && !finalToken.isEmpty()) {
                        System.out.println("🎯 完美！成功使用 ST 兑换到终极 fighter-auth-token!");
                        return finalToken;
                    } else {
                        System.err.println("❌ 接口返回 200，但解析不到 data 字段。返回原文: " + apiResponse.text());
                    }
                } else {
                    System.err.println("❌ 兑换 Token 的接口报错，状态码: " + apiResponse.status());
                }

            } catch (TimeoutError e) {
                System.err.println("❌ 等待 URL 出现 ticket 超时，页面最终停留在: " + page.url());
            }

            return null;
        } finally {
            // ================== Spring Boot 定时任务 ClassLoader 修复 ==================
            // 无论成功还是失败，必须还原原始的 ClassLoader，防止影响 Spring 的其他线程池任务
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * 调用本地 Python 脚本识别验证码
     */
    private static String recognizeCaptchaWithPython(String imagePath) {
        StringBuilder result = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "D:\\javaCode\\ding-checkin-java\\src\\main\\java\\cn\\hhdxt\\dingcheckinjava\\utils\\ocr_tool.py", imagePath);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line.trim());
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("❌ 调用 Python OCR 脚本失败！");
            e.printStackTrace();
        }
        return result.toString();
    }



    private static String getStudentId(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://of.swu.edu.cn/gateway/fighter-middle/api/auth/user?appType=fighter-portal"))
                .header("fighter-auth-token", token)
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body()).path("data").path("subject").path("username").asText();
    }

    private static JsonNode getTransitionToday(String token) throws Exception {
        // 保留之前的逻辑...
        String body = "pageNum=1&pageSize=1";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://of.swu.edu.cn//gateway/fighter-baida/api/cqtj/getTransitionByToday"))
                .header("fighter-auth-token", token)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode records = mapper.readTree(response.body()).path("data").path("records");

        return records.isArray() && records.size() > 0 ? records.get(0) : null;
    }

    // ================= 对外暴露的启动入口 =================
    public static boolean start(String username, String password) {
        System.out.println("🚀 启动自动打卡程序...");
        try {
            // 直接返回 checkin 的执行结果 (true/false)
            return checkin(username, password);
        } catch (Exception e) {
            System.err.println("❌ 启动过程中发生严重异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}