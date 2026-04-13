package cn.hhdxt.dingcheckinjava.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户账号表 前端控制器
 * </p>
 *
 * @author HHDXT
 * @since 2026-03-02
 */
@RestController
@RequestMapping("/user")
public class UserAccountController {


    @GetMapping
    public String test(){
        return "test";
    }

}
