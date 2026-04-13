package cn.hhdxt.dingcheckinjava.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CaptchaResolver {

    public static String getCaptcha(String imageUrl) {
        StringBuilder result = new StringBuilder();
        try {
            // 指定 python 解释器路径和脚本路径
            String pythonPath = "python"; // 或者 "python3" 或具体的 exe 路径
            String scriptPath = "D:\\javaCode\\ding-checkin-java\\src\\main\\java\\cn\\hhdxt\\dingcheckinjava\\utils\\ocr_tool.py";

            // 构建命令
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, imageUrl);
            pb.redirectErrorStream(true); // 合并错误流到标准输出

            Process process = pb.start();

            // 读取 Python 脚本的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
        return result.toString().trim();
    }

    public static String getCode() {
        String url = "https://idm.swu.edu.cn/am/validate.code"; // 替换为真实的验证码URL
        //        System.out.println("识别到的验证码是: " + code);
        return getCaptcha(url);
    }

}