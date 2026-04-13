package cn.hhdxt.dingcheckinjava.service.impl;

import cn.hhdxt.dingcheckinjava.properties.accountProperties;
import cn.hhdxt.dingcheckinjava.service.IUserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户账号表 服务实现类
 * </p>
 *
 * @author HHDXT
 * @since 2026-03-02
 */
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements IUserAccountService {

    private final accountProperties accountProperties;

    public List<Map<String,String>> getUserAccount(){

        return accountProperties.getUserAccount();
    }

}
