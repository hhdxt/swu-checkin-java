package cn.hhdxt.dingcheckinjava.utils;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

//@Component
public class MyTestJob {

    /**
     * 1. 简单任务示例
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        // 使用 XxlJobHelper.log 代替 System.out.println
        // 这样日志会同步到 XXL-JOB 管理后台，方便远程查看
        XxlJobHelper.log("XXL-JOB, Hello World.");

        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("beat at index: " + i);
            TimeUnit.SECONDS.sleep(1);
        }
        
        // 默认任务成功，如果抛出异常则任务失败
    }
}