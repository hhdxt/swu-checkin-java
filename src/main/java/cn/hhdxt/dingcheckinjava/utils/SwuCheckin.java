package cn.hhdxt.dingcheckinjava.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
public class SwuCheckin {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();

    public static int checkin(String username, String password) {
        if (username == null || password == null) {
            log.error("缺少用户名或密码");
            return -1;
        }

        try {
            log.debug("========== [1] 开始通过浏览器获取授权 Token ==========");
            String token = getTokenWithPlaywright(username, password);
            if (token == null || token.isEmpty()) {
                log.error("获取 Token 失败，打卡终止");
                return -1;
            }
            log.info("成功拿到打卡通行证 Token: {}", token);

            log.debug("========== [2] 开始查询今日打卡任务 ==========");
            JsonNode transitionToday = getTransitionToday(token);

            if (transitionToday == null || transitionToday.isEmpty()) {
                log.info("暂无打卡任务");
                return 0;
            }

            if ("已签到".equals(transitionToday.path("qdzt").asText())) {
                log.info("今日已打卡，无需重复提交");
                return 2;
            }

            log.debug("========== [3] 执行最终打卡提交 ==========");
            checkin(token, transitionToday);
            log.info("打卡成功");
            return 1;

        } catch (Exception e) {
            log.error("运行过程中发生系统级异常: {}", e.getMessage());
            return -1;
        }
    }


    private static void checkin(String token, JsonNode transition) throws Exception {
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

    private static String getTokenWithPlaywright(String username, String password) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(SwuCheckin.class.getClassLoader());

        try (Playwright playwright = Playwright.create()) {
            log.debug("正在初始化 Playwright 引擎...");
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setChannel("chrome")
                    .setIgnoreDefaultArgs(Arrays.asList("--enable-automation"))
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled",
                            "--window-size=1280,720"
                    ));
            Browser browser = playwright.chromium().launch(options);

            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
            Page page = context.newPage();

            String loginUrl = "https://of.swu.edu.cn/cas/oauth/login/SWU_CAS2_FEDERAL?service=https%3A%2F%2Fof.swu.edu.cn%2Fgateway%2Ffighter-middle%2Fapi%2Fintegrate%2Fuaap%2Fcas%2Fresolve-cas-return%3Fnext%3Dhttps%253A%252F%252Fof.swu.edu.cn%252F%2523%252FcasLogin%253Ffrom%253D%25252FappCenter";
            log.debug("开始访问登录入口...");

            page.navigate(loginUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(3000);

            try {
                log.debug("检查是否需要点击中间页的【统一认证按钮】...");
                page.waitForSelector(".loginMethd", new Page.WaitForSelectorOptions().setTimeout(5000));
                log.debug("发现中间页按钮，正在点击...");
                page.click(".loginMethd");

                log.debug("等待真实账号输入框加载...");
                page.waitForSelector("#loginName", new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (TimeoutError e) {
                log.debug("未发现中间页按钮或超时，检查页面是否已直接进入账号密码界面...");
            }

            page.waitForTimeout(1000);

            boolean loginSuccess = false;
            int maxRetries = 3;

            for (int i = 1; i <= maxRetries; i++) {
                log.info("尝试第 {} 次登录", i);
                Locator captchaLocator = page.locator("#kaptchaImage");

                if (!captchaLocator.isVisible()) {
                    log.error("找不到验证码图片！可能被防护拦截或页面结构发生改变");
                    throw new RuntimeException("页面未渲染出验证码");
                }

                String captchaPath = "captcha.png";
                captchaLocator.screenshot(new Locator.ScreenshotOptions().setPath(Paths.get(captchaPath)));
                log.debug("验证码截图保存成功，正在调用 Python OCR...");

                String code = recognizeCaptchaWithPython(captchaPath);
                log.debug("OCR 识别结果: [{}]", code);

                log.debug("正在填充账号、密码、验证码...");
                page.fill("#loginName", username);
                page.fill("#password", password);
                page.fill("#validateCode", code);

                page.waitForTimeout(500);
                log.debug("点击登录按钮...");
                page.click("#button");

                try {
                    log.debug("正在等待登录响应结果...");
                    page.waitForCondition(() -> !page.url().contains("am/UI/Login"), new Page.WaitForConditionOptions().setTimeout(8000));
                    log.debug("URL 发生变化，判定登录表单提交成功！新 URL: {}", page.url());
                    loginSuccess = true;
                    break;
                } catch (TimeoutError e) {
                    log.warn("登录未发生跳转！当前 URL 仍然是: {}", page.url());
                    log.warn("准备清空密码并刷新验证码重试...");
                    page.fill("#password", "");
                    captchaLocator.click();
                    page.waitForTimeout(1500);
                }
            }

            if (!loginSuccess) {
                throw new RuntimeException("连续 " + maxRetries + " 次验证码识别或登录失败");
            }

            log.debug("登录成功，正在紧盯 URL 等待 CAS 颁发票据 (Ticket)...");

            try {
                page.waitForCondition(() -> page.url().contains("ticket="), new Page.WaitForConditionOptions().setTimeout(10000));

                String currentUrl = page.url();
                log.debug("成功捕获重定向 URL: {}", currentUrl);

                String ticket = currentUrl.split("ticket=")[1].split("&")[0];
                log.debug("成功抠出服务票据 (Service Ticket): {}", ticket);

                String exchangeUrl = "https://of.swu.edu.cn/gateway/fighter-middle/api/integrate/uaap/cas/exchange-token?token=" + ticket + "&remember=true";
                log.debug("正在主动请求后端接口兑换 Token...");

                com.microsoft.playwright.APIResponse apiResponse = context.request().get(exchangeUrl);

                if (apiResponse.status() == 200) {
                    JsonNode rootNode = mapper.readTree(apiResponse.text());
                    String finalToken = rootNode.path("data").asText();

                    if (finalToken != null && !finalToken.isEmpty()) {
                        log.debug("成功使用 ST 兑换到终极 fighter-auth-token!");
                        return finalToken;
                    } else {
                        log.error("接口返回 200，但解析不到 data 字段。返回原文: {}", apiResponse.text());
                    }
                } else {
                    log.error("兑换 Token 的接口报错，状态码: {}", apiResponse.status());
                }

            } catch (TimeoutError e) {
                log.error("等待 URL 出现 ticket 超时，页面最终停留在: {}", page.url());
            }

            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private static String recognizeCaptchaWithPython(String imagePath) {
        StringBuilder result = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "/app/ocr_tool.py", imagePath);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line.trim());
            }
            process.waitFor();
        } catch (Exception e) {
            log.error("调用 Python OCR 脚本失败！");
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

    public static int start(String username, String password) {
        log.info("启动自动打卡程序...");
        try {
            return checkin(username, password);
        } catch (Exception e) {
            log.error("启动过程中发生严重异常: {}", e.getMessage());
            return -1;
        }
    }
}