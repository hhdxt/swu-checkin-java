package cn.hhdxt.dingcheckinjava.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class BillionMailUtil {

    private static final String API_URL = "http://mail.email.tygccg.xyz/api/batch_mail/api/send";


    private static final HttpClient HTTP_CLIENT = createUnsafeHttpClient();

    static {
        // 【关键】禁用 Java HttpClient 的主机名验证，等同于 curl -k
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }

    /**
     * 创建一个忽略 SSL 证书校验的 HttpClient (等同于 curl -k)
     */
    private static HttpClient createUnsafeHttpClient() {
        try {
            // 创建一个信任所有证书的 TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(sslContext) // 注入忽略证书的上下文
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("初始化 HttpClient 失败", e);
        }
    }

    public static String sendMail(String recipient,String API_KEY) {
        // 构建 JSON
        String jsonBody = "{\"recipient\": \"" + recipient + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("X-API-Key", API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return "状态码: " + response.statusCode() + " | 响应: " + response.body();
        } catch (Exception e) {
            // 打印堆栈信息以便排查具体原因
            e.printStackTrace();
            return "请求异常: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }
}
//    public static void main(String[] args) {
//        System.out.println(sendMail("f2236607434@email.swu.edu.cn"));
//    }
//}