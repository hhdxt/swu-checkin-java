package cn.hhdxt.dingcheckinjava.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    // --- 关键点 1：注入映射后的公网地址 ---
    @Value("${xxl.job.executor.ip}")
    private String ip;

    // --- 关键点 2：注入映射后的公网端口 ---
    @Value("${xxl.job.executor.port}")
    private int port;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAccessToken(accessToken);

        // --- 核心设置：必须显式 set ---
        xxlJobSpringExecutor.setIp(ip);   // 设为 frp.tygccg.xyz
        xxlJobSpringExecutor.setPort(port); // 设为 18081（frp 的 remote_port）

        return xxlJobSpringExecutor;
    }
}