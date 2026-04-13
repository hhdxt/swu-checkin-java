package cn.hhdxt.dingcheckinjava.service.impl;

import cn.hhdxt.dingcheckinjava.service.IUserAccountService;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * <p>
 * 用户账号表 服务实现类
 * </p>
 *
 * @author HHDXT
 * @since 2026-03-02
 */
@Service
public class UserAccountServiceImpl implements IUserAccountService {

    public HashMap<String,String> getUserAccount(){
        HashMap<String,String> userAccount = new HashMap<>();

        userAccount.put("username","f2236607434");
        userAccount.put("password","fxj20060127.");
        return userAccount;
    }

}
