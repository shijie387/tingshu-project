package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

public interface UserAccountService extends IService<UserAccount> {


    void saveUserAccount(Map<String, Object> mapData);

    void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo);
}
