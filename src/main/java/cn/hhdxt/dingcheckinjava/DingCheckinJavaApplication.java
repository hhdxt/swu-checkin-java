package cn.hhdxt.dingcheckinjava;

import cn.hhdxt.dingcheckinjava.demo.SwuCheckinDemo;
import cn.hhdxt.dingcheckinjava.service.IUserAccountService;
import cn.hhdxt.dingcheckinjava.utils.BillionMailUtil;
import cn.hhdxt.dingcheckinjava.utils.SwuCheckin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class})
@EnableScheduling  // 开启定时任务
public class DingCheckinJavaApplication {

    // TODO: 请替换为你的BillionMail邮件API密钥 (https://www.niui.cn/)
    // 成功发送邮件时的 API Key
    private static final String API_KEY_SUCCESS = "你的BillionMailAPI密钥-成功";
    // 发送失败邮件时的 API Key
    private static final String API_KEY_FAILURE = "你的BillionMailAPI密钥-失败";


    private final IUserAccountService userAccountService;


    @Scheduled(cron = "0 10 21 * * ?")
    public void scheduledTask() {

        List<Map<String, String>> mapList = userAccountService.getUserAccount();
        for (Map<String, String> map : mapList) {

            for (int i = 0; i < 3; i++) {
                try {
                    int result = SwuCheckin.start(map.get("username"), map.get("password"));
                    switch (result) {
                        case 1:
                            log.info("第{}次登录成功 - 打卡成功", i + 1);
                            BillionMailUtil.sendMail(map.get("email"), API_KEY_SUCCESS);
                            return;
                        case 2:
                            log.info("第{}次登录 - 今日已打卡", i + 1);
                            return;
                        case 0:
                            log.info("第{}次登录 - 暂无打卡任务", i + 1);
                            return;
                        case -1:
                        default:
                            log.error("第{}次登录失败", i + 1);
                            if (i < 2) {
                                log.info("准备第{}次重试...", i + 2);
                            }
                            break;
                    }
                } catch (Exception e) {
                    log.error("打卡异常: {}", e.getMessage());
                    BillionMailUtil.sendMail(map.get("email"), API_KEY_FAILURE);
                }
            }
            log.error("重试3次后仍失败，发送失败邮件");
            BillionMailUtil.sendMail(map.get("email"), API_KEY_FAILURE);
        }


    }

    public DingCheckinJavaApplication(IUserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DingCheckinJavaApplication.class, args);
        log.info("==========启动成功==========");
    }

}
