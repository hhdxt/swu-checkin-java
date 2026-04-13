package cn.hhdxt.dingcheckinjava.utils;

import cn.hhdxt.dingcheckinjava.utils.DesUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SwuCheckin {

    private static final ObjectMapper mapper = new ObjectMapper();


    // 显式指定接受所有 Cookie，以完美模拟 requests.Session() 的行为
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();

    public static boolean checkin(String username, String password) {


        if (username == null || password == null) {
            log.error("缺少用户名或密码。请通过环境变量或命令行参数提供。");
            return false;
        }

        try {
            if (!login(username, password)) {
                log.error("登录失败，账号或密码错误");
                return false;
            }

            String token = getToken(username, password);
            JsonNode transitionToday = getTransitionToday(token);

            if (transitionToday == null || transitionToday.isEmpty()) {
                log.info("暂无打卡任务");
                return false;
            }

            if ("已签到".equals(transitionToday.path("qdzt").asText())) {
                log.info("今日已打卡");
                return false;
            }

            checkin(token, transitionToday);
            log.info("打卡成功");
            return true;

        } catch (Exception e) {
            log.error("运行过程中发生错误: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean login(String username, String password) throws Exception {
        String[] enc = desEncrypt(username, password);
        String encUser = enc[0];
        String encPass = enc[1];

        // 完整的 goto 参数
        String gotoStr = "aHR0cDovL2lkbS5zd3UuZWR1LmNuL2FtL29hdXRoMi9hdXRob3JpemU/c2VydmljZT1pbml0U2VydmljZSZyZXNwb25zZV90eXBlPWNvZGUmY2xpZW50X2lkPTdjMXpva29samw5YmJpaG82eXVvJnNjb3BlPXVpZCtjbit1c2VySWRDb2RlJnJlZGlyZWN0X3VyaT1odHRwcyUzQSUyRiUyRnVhYWFwLnN3dS5lZHUuY24lMkZjYXMlMkZsb2dpbiUzRnNlcnZpY2UlM0RodHRwcyUyNTNBJTI1MkYlMjUyRnVhYWFwLnN3dS5lZHUuY24lMjUyRmNhcyUyNTJGb2F1dGgyLjAlMjUyRmNhbGxiYWNrQXV0aG9yaXplJTI2b3JpZ2luYWxSZXF1ZXN0VXJsJTNEaHR0cHMlMjUzQSUyNTJGJTI1MkZ1YWFhcC5zd3UuZWR1LmNuJTI1MkZjYXMlMjUyRm9hdXRoMi4wJTI1MkZhdXRob3JpemUlMjUzRnJlc3BvbnNlX3R5cGUlMjUzRGNvZGUlMjUyNmNsaWVudF9pZCUyNTNEY2FzNiUyNTI2cmVkaXJlY3RfdXJpJTI1M0RodHRwcyUyNTI1M0ElMjUyNTJGJTI1MjUyRm9mLnN3dS5lZHUuY24lMjUyNTNBNDQzJTI1MjUyRmNhcyUyNTI1MkZvYXV0aCUyNTI1MkZjYWxsYmFjayUyNTI1MkZTV1VfQ0FTMl9GRURFUkFMJTI1MjZzdGF0ZSUyNTNEZTFlMTczODhlNzU4MjY3YjFiNzI2ZjM4Mjg0NDM5MWElMjUyNnNjb3BlJTI1M0RzaW1wbGUlMjZmZWRlcmFsRW5hYmxlJTNEdHJ1ZSZkZWNpc2lvbj1BbGxvdw==";

        String sunQueryParamsString = SwuLoginExtractor.getInfo().get("sunParam");
        if(sunQueryParamsString==null){
            throw new RuntimeException("随机字符串未成功获取");
        }
        String code = CaptchaResolver.getCode();
        verify(code);


        String body = String.format("IDToken1=%s&IDToken2=%s&IDToken3=&goto=%s&gotoOnFail=&sunQueryParamsString=%s&encoded=true&validataCode=%s&gx_charset=UTF-8",
                URLEncoder.encode(encUser, StandardCharsets.UTF_8),
                URLEncoder.encode(encPass, StandardCharsets.UTF_8),
                URLEncoder.encode(gotoStr, StandardCharsets.UTF_8),
                URLEncoder.encode(sunQueryParamsString, StandardCharsets.UTF_8),
                URLEncoder.encode(code, StandardCharsets.UTF_8)
        );
        log.info(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://idm.swu.edu.cn/am/UI/Login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpClient noRedirectClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpResponse<String> response = noRedirectClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info(response.body());

        return response.statusCode() == 302;
    }

    private static String getToken(String username, String password) throws Exception {
        String[] enc = desEncrypt(username, password);

        // 完整的 goto 参数，不能省略
        String gotoStr = "aHR0cDovL2lkbS5zd3UuZWR1LmNuL2FtL29hdXRoMi9hdXRob3JpemU/c2VydmljZT1pbml0U2VydmljZSZyZXNwb25zZV90eXBlPWNvZGUmY2xpZW50X2lkPTdjMXpva29samw5YmJpaG82eXVvJnNjb3BlPXVpZCtjbit1c2VySWRDb2RlJnJlZGlyZWN0X3VyaT1odHRwcyUzQSUyRiUyRnVhYWFwLnN3dS5lZHUuY24lMkZjYXMlMkZsb2dpbiUzRnNlcnZpY2UlM0RodHRwcyUyNTNBJTI1MkYlMjUyRnVhYWFwLnN3dS5lZHUuY24lMjUyRmNhcyUyNTJGb2F1dGgyLjAlMjUyRmNhbGxiYWNrQXV0aG9yaXplJTI2b3JpZ2luYWxSZXF1ZXN0VXJsJTNEaHR0cHMlMjUzQSUyNTJGJTI1MkZ1YWFhcC5zd3UuZWR1LmNuJTI1MkZjYXMlMjUyRm9hdXRoMi4wJTI1MkZhdXRob3JpemUlMjUzRnJlc3BvbnNlX3R5cGUlMjUzRGNvZGUlMjUyNmNsaWVudF9pZCUyNTNEY2FzNiUyNTI2cmVkaXJlY3RfdXJpJTI1M0RodHRwcyUyNTI1M0ElMjUyNTJGJTI1MjUyRm9mLnN3dS5lZHUuY24lMjUyNTNBNDQzJTI1MjUyRmNhcyUyNTI1MkZvYXV0aCUyNTI1MkZjYWxsYmFjayUyNTI1MkZTV1VfQ0FTMl9GRURFUkFMJTI1MjZzdGF0ZSUyNTNEZTFlMTczODhlNzU4MjY3YjFiNzI2ZjM4Mjg0NDM5MWElMjUyNnNjb3BlJTI1M0RzaW1wbGUlMjZmZWRlcmFsRW5hYmxlJTNEdHJ1ZSZkZWNpc2lvbj1BbGxvdw==";

        String sunQueryParamsString = SwuLoginExtractor.getInfo().get("sunParam");
        if(sunQueryParamsString==null){
            throw new RuntimeException("随机字符串未成功获取");
        }
        String body = String.format("IDToken1=%s&IDToken2=%s&IDToken3=&goto=%s&gotoOnFail=&sunQueryParamsString=%s&encoded=true&gx_charset=UTF-8",
                URLEncoder.encode(enc[0], StandardCharsets.UTF_8),
                URLEncoder.encode(enc[1], StandardCharsets.UTF_8),
                URLEncoder.encode(gotoStr, StandardCharsets.UTF_8),
                URLEncoder.encode(sunQueryParamsString, StandardCharsets.UTF_8)
        );

        // 1. 获取登录状态 (获取 session 关联的 state)
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create("https://of.swu.edu.cn/cas/oauth/login/SWU_CAS2_FEDERAL?service=https%3A%2F%2Fof.swu.edu.cn%2Fgateway%2Ffighter-middle%2Fapi%2Fintegrate%2Fuaap%2Fcas%2Fresolve-cas-return%3Fnext%3Dhttps%253A%252F%252Fof.swu.edu.cn%252F%2523%252FcasLogin%253Ffrom%253D%25252FappCenter"))
                .GET().build();

        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        String finalUrl = URLDecoder.decode(getResp.uri().toString(), StandardCharsets.UTF_8);

        // 安全获取 state
        if (!finalUrl.contains("state=")) {
            throw new Exception("第一步初始化失败：无法在重定向 URL 中找到 state。当前 URL: " + finalUrl);
        }
        String state = finalUrl.split("state=")[1].substring(0, 32);

        // 2. 提交登录表单
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create("https://idm.swu.edu.cn/am/UI/Login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString());

        String urlAfterLogin = URLDecoder.decode(postResp.uri().toString(), StandardCharsets.UTF_8);
        if (!urlAfterLogin.contains("ticket=")) {
            // 这里为了方便排查，输出最终的跳转 URL
            throw new Exception("登录失败：无法获取票据参数。认证系统最终停留地址: " + urlAfterLogin);
        }
        String ticket = urlAfterLogin.split("ticket=")[1];
        String[] transformed = transform(ticket);

        // 3. 回调置换
        String CD = String.format("CD-%s-%s-wiie://777.643.675.751:3537/rph", transformed[0], transformed[1]);
        String callbackUrl = String.format("https://of.swu.edu.cn/cas/oauth/callback/SWU_CAS2_FEDERAL?code=%s@@hxbeat&state=%s", CD, state);

        HttpRequest callbackReq = HttpRequest.newBuilder().uri(URI.create(callbackUrl)).GET().build();
        HttpResponse<String> callbackResp = client.send(callbackReq, HttpResponse.BodyHandlers.ofString());

        String stUrl = callbackResp.uri().toString();
        if (!stUrl.contains("ticket=")) {
            throw new Exception("登录失败：回调后无法获取ST参数。当前 URL: " + stUrl);
        }
        String ST = stUrl.split("ticket=")[1];

        // 4. 获取 Token
        HttpRequest tokenReq = HttpRequest.newBuilder()
                .uri(URI.create("https://of.swu.edu.cn/gateway/fighter-middle/api/integrate/uaap/cas/exchange-token?token=" + ST + "&remember=true"))
                .GET().build();
        HttpResponse<String> tokenResp = client.send(tokenReq, HttpResponse.BodyHandlers.ofString());

        JsonNode rootNode = mapper.readTree(tokenResp.body());
        if (!rootNode.has("data") || rootNode.path("data").isNull()) {
            throw new Exception("获取令牌失败，响应内容不包含 data 字段: " + tokenResp.body());
        }
        return rootNode.path("data").asText();
    }

    // 后续的 transform、getTransitionToday、checkin 等方法保持原样...
    // 此处省略为了节约篇幅，请保留你现有的逻辑。

    private static String[] transform(String ticket) {
        // 保留之前的逻辑
        String decoded = URLDecoder.decode(ticket, StandardCharsets.UTF_8);
        String[] parts = decoded.split("-");

        StringBuilder str1 = new StringBuilder();
        for (char c : parts[1].toCharArray()) {
            str1.append((Character.getNumericValue(c) + 5) % 10);
        }

        StringBuilder str2 = new StringBuilder();
        for (char c : parts[2].toCharArray()) {
            if (c >= '0' && c <= '9') {
                str2.append((Character.getNumericValue(c) + 5) % 10);
            } else if (c >= 'A' && c <= 'Z') {
                int shift = c + 10;
                if (shift > 'Z') shift -= 26;
                str2.append((char) shift);
            } else if (c >= 'a' && c <= 'z') {
                int shift = c + 15;
                if (shift > 'z') shift -= 26;
                str2.append((char) shift);
            } else {
                str2.append(c);
            }
        }
        return new String[]{str1.toString(), str2.toString()};
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

    private static String getStudentId(String token) throws Exception {
        // 保留之前的逻辑...
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://of.swu.edu.cn/gateway/fighter-middle/api/auth/user?appType=fighter-portal"))
                .header("fighter-auth-token", token)
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body()).path("data").path("subject").path("username").asText();
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

    private static String[] desEncrypt(String username, String password) {
//        String randomKey = "OqxQ1Iea4njSROH/N06Tuw==";

        String randomKey = SwuLoginExtractor.getInfo().get("randomParam");
        return new String[]{
                DesUtils.strEnc(username, randomKey, "", ""),
                DesUtils.strEnc(password, randomKey, "", "")
        };
    }

    /**
     * @param validateCode 验证码
     *
     */
    public static void verify(String validateCode) {
        // 1. 完整的 URL（注意：末尾的随机字符串 ZUY2FAwZ 最好从页面实时获取，这里先演示你提供的）
        String url = "https://idm.swu.edu.cn/am/validatecode/verify.do?ZUY2FAwZ=0lFyBmalqEH2cOZFLtA2kYnKreMPfQoPYa0fq4eUG1Yj1FNNiLaZQEoGwm3fPucFZgP4vCnW7vWhj3gFST0u8NN76H4sRyrBO";

        // 2. 创建 HttpClient
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 3. 构建 Body (application/x-www-form-urlencoded)
        String formBody = "validateCode=" + validateCode;

        // 4. 构建 Request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("X-Requested-With", "XMLHttpRequest")
//                .header("Cookie", cookieValue) // 必须传入 Cookie
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/137.0.0.0 Safari/537.36")
                .header("Referer", "https://idm.swu.edu.cn/am/UI/Login?service=initService&encoded=false")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        // 5. 发送请求并处理结果
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("HTTP 状态码: " + response.statusCode());
                    return response.body();
                })
                .thenAccept(body -> {
                    System.out.println("服务器返回结果: " + body);
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                })
                .join(); // 等待异步完成（仅用于测试脚本）
    }
}