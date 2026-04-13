package cn.hhdxt.dingcheckinjava.controller;


import cn.hhdxt.dingcheckinjava.service.IUserAccountService;
import cn.hhdxt.dingcheckinjava.utils.BillionMailUtil;
import cn.hhdxt.dingcheckinjava.utils.SwuCheckin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户账号表 前端控制器
 * </p>
 *
 * @author HHDXT
 * @since 2026-03-02
 */
@RestController
@RequestMapping("/test")
@Slf4j
@RequiredArgsConstructor
public class UserAccountController {

    private final IUserAccountService userAccountService;


    @GetMapping
    public String test(){



        try {
            int result = SwuCheckin.start("f2236607434", "fxj20060127.");
            switch (result) {
                case 1:
                    return ("打卡成功");
                case 2:
                    return (" 今日已打卡");
//                        BillionMailUtil.sendMail("f2236607434@email.swu.edu.cn", API_KEY_FAILURE);
                case 0:
                    return ("- 暂无打卡任务");
//                        BillionMailUtil.sendMail("f2236607434@email.swu.edu.cn", API_KEY_FAILURE);
                case -1:

            }
        } catch (Exception e) {
            return ("打卡异常: {}"+ e.getMessage());
        }
        return "";
    }

}
