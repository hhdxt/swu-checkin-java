package cn.hhdxt.dingcheckinjava.service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户账号表 服务类
 * </p>
 *
 * @author HHDXT
 * @since 2026-03-02
 */
public interface IUserAccountService  {

    List<Map<String,String>> getUserAccount();
}
