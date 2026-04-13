package cn.hhdxt.dingcheckinjava.demo;

import com.microsoft.playwright.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;

public class SWULoginDemo {

    public static void main(String[] args) {
        // 1. 初始化 Playwright，建议本地调试时关闭 headless，看着它跑
        try (Playwright playwright = Playwright.create()) {
            // 1. 核心伪装：添加启动参数，禁用自动化控制特征
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled", // 关键：禁用自动化标志
                            "--disable-infobars" // 隐藏"Chrome正受到自动测试软件的控制"的提示
                    ));
            Browser browser = playwright.chromium().launch(launchOptions);

            // 2. 配置上下文：伪造 User-Agent 并忽略可能拦截请求的 SSL 错误
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setIgnoreHTTPSErrors(true) // 忽略 HTTPS 证书警告
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"); // 使用真实的 UA
            BrowserContext context = browser.newContext(contextOptions);

            // 3. 核心伪装：在页面加载任何脚本之前，注入 JS 抹除 webdriver 特征
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            Page page = context.newPage();

            System.out.println("正在打开西南大学统一认证页面...");
            // 4. 加载页面 (调整了等待策略，防止被重定向卡住)
            page.navigate("https://idm.swu.edu.cn/am/UI/Login?service=initService&encoded=false", new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));
            // 稍微等待一下，让瑞数的 JS 充分执行完毕并渲染出真实的输入框
            page.waitForTimeout(2000);

            // 2. 找到验证码图片元素，并截图保存到本地
            String captchaImgPath = "captcha.png";
            Locator captchaLocator = page.locator("#kaptchaImage");
            captchaLocator.screenshot(new Locator.ScreenshotOptions().setPath(Paths.get(captchaImgPath)));
            System.out.println("验证码已截取并保存至: " + captchaImgPath);

            // 3. 调用本地 Python 脚本进行识别
            String validateCode = recognizeCaptchaWithPython("D:\\javaCode\\ding-checkin-java\\captcha.png");
            System.out.println("Python 脚本识别结果: " + validateCode);

            // 4. 将账号、密码和识别到的验证码填入页面
            page.fill("#loginName", "f2236607434");
            page.fill("#password", "fxj20060127.");
            
            // 填入验证码
            page.fill("#validateCode", validateCode);

            // 5. 点击登录按钮 (根据你提供的HTML，按钮ID是 button)
            System.out.println("信息填写完毕，准备点击登录...");
            page.click("#button");

            // TODO: 这里可以加上等待页面跳转的逻辑，并获取成功登录后的 Cookie
            // page.waitForNavigation(() -> {});
            // System.out.println(page.context().cookies());

            // 调试用：让浏览器停留几秒看看效果再关
            Thread.sleep(5000); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 调用本地 Python 脚本
     */
    private static String recognizeCaptchaWithPython(String imagePath) {
        StringBuilder result = new StringBuilder();
        try {
            // 构建命令：python recognize.py captcha.png
            // 注意：如果在服务器上，python 可能需要改成 python3，或者写死虚拟环境的绝对路径
            ProcessBuilder pb = new ProcessBuilder("python", "D:\\javaCode\\ding-checkin-java\\src\\main\\java\\cn\\hhdxt\\dingcheckinjava\\utils\\ocr_tool.py", imagePath);
            Process process = pb.start();

            // 读取 Python 脚本的标准输出 (System.out.print)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // 假设 Python 脚本直接打印了 4 位数字
                result.append(line.trim());
            }

            // 等待 Python 进程执行完毕
            process.waitFor();
            
        } catch (Exception e) {
            System.err.println("调用 Python 脚本失败！");
            e.printStackTrace();
        }
        return result.toString();
    }
}