package cn.hhdxt.dingcheckinjava.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwuLoginExtractor {

    /**
     * 1. 发送 HTTP 请求并获取完整的 HTML 页面内容
     */
    public static String fetchLoginPageHtml() {
        String targetUrl = "https://idm.swu.edu.cn/am/UI/Login?service=initService&encoded=false";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("accept-language", "zh-CN,zh;q=0.9")
                    .header("cache-control", "no-cache")
                    .header("pragma", "no-cache")
                    .header("sec-ch-ua", "\"Google Chrome\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Windows\"")
                    .header("sec-fetch-dest", "document")
                    .header("sec-fetch-mode", "navigate")
                    .header("sec-fetch-site", "none")
                    .header("sec-fetch-user", "?1")
                    .header("upgrade-insecure-requests", "1")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("请求失败，HTTP 状态码: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 2. 从 HTML 中提取 SunQueryParamsString 的值
     */
    public static String extractSunQueryParamsString(String html) {
        if (html == null || html.isEmpty()) return null;

        Pattern pattern = Pattern.compile("name=[\"']SunQueryParamsString[\"']\\s+value=[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 3. 新增：从 HTML 中提取 random 的值
     */
    public static String extractRandom(String html) {
        if (html == null || html.isEmpty()) return null;

        // 匹配模式: name="random" value="需要提取的值"
        Pattern pattern = Pattern.compile("name=[\"']random[\"']\\s+value=[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1); // 返回提取到的 Base64 字符串
        }
        return null;
    }

    public static Map<String,String> getInfo() {
        // 第一步：只发送一次请求获取完整 HTML 数据
        String htmlData = fetchLoginPageHtml();

        if (htmlData != null) {
            // 第二步：在同一个数据中分别提取两个参数
            String sunParam = extractSunQueryParamsString(htmlData);
            String randomParam = extractRandom(htmlData);


            Map<String,String> map = new HashMap<>();
            map.put("sunParam", sunParam);
            map.put("randomParam", randomParam);
//            System.out.println("提取到的 SunQueryParamsString 为: " + sunParam);
//            System.out.println("提取到的 random 为: " + randomParam);
            return map;
        } else {
//            System.out.println("获取网页 HTML 失败，无法提取参数。");
            throw new RuntimeException("获取信息失败");
        }
    }
}