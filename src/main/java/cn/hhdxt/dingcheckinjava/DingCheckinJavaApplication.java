package cn.hhdxt.dingcheckinjava;

import cn.hhdxt.dingcheckinjava.demo.SwuCheckinDemo;
import cn.hhdxt.dingcheckinjava.service.IUserAccountService;
import cn.hhdxt.dingcheckinjava.utils.BillionMailUtil;
import cn.hhdxt.dingcheckinjava.utils.SwuCheckin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.HashMap;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class})
@EnableScheduling  // 开启定时任务
public class DingCheckinJavaApplication {

    private static final String API_KEY_SUCCESS = "d8c8a120199eefe6fb62eb8c4347618e914128f76d3ddda882c4381d9f989dc2";
    private static final String API_KEY_FAILURE = "644bd3621992e6e47486fffb53e5460cc12bb0f405a54142ea0d391d09dfd1c1";


    private final IUserAccountService userAccountService;

    public DingCheckinJavaApplication(IUserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DingCheckinJavaApplication.class, args);
        System.out.println("==========启动成功==========");
    }

    // Cron表达式：每天21:10执行一次
    @Scheduled(cron = "0 10 21 * * ?")
    public void scheduledTask() {

        // 尝试登录3次
        for (int i = 0; i < 3; i++) {
            // 调用登录方法
            HashMap<String, String> userAccount = userAccountService.getUserAccount();
            try {
//                boolean flag = SwuCheckin.checkin(userAccount.get("username"), userAccount.get("password"));
                boolean flag = SwuCheckinDemo.start("f2236607434", "fxj20060127.");
                if (flag) {
                    System.out.println("第" + (i + 1) + "次登录成功" + LocalDateTime.now());
                    // 1. 发送普通成功邮件
                    BillionMailUtil.sendMail("f2236607434@email.swu.edu.cn", API_KEY_SUCCESS);
                    break;
                }
            } catch (Exception e) {
                // 1. 发送普通失败邮件
                System.out.println("打卡失败");
                BillionMailUtil.sendMail("f2236607434@email.swu.edu.cn", API_KEY_FAILURE);
                throw new RuntimeException(e);
            }

        }
    }

}
