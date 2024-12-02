package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;

	@Autowired
	private UserAccountDetailMapper userAccountDetailMapper;
	/**
	 * 初始化账户记录；新增账户变动日志
	 * @param mapData {"userId",1,"title":"","amount":10,"orderNo":"cz001"}
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveUserAccount(Map mapData) {
		//1.获取从MQ中获取到参数
		Long userId = (Long) mapData.get("userId");
		String title = (String) mapData.get("title");
		BigDecimal amount = (BigDecimal) mapData.get("amount");
		String orderNo = (String) mapData.get("orderNo");

		//2.保存账户记录
		UserAccount userAccount = new UserAccount();
		userAccount.setUserId(userId);
		userAccount.setTotalAmount(amount);
		userAccount.setAvailableAmount(amount);
		userAccount.setTotalIncomeAmount(amount);
		userAccountMapper.insert(userAccount);
		//3.新增账户变动日志
		this.saveUserAccountDetail(userId, title, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, amount, orderNo);

	}

	@Override
	public void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {
		UserAccountDetail userAccountDetail = new UserAccountDetail();
		userAccountDetail.setUserId(userId);
		userAccountDetail.setTitle(title);
		userAccountDetail.setTradeType(tradeType);
		userAccountDetail.setAmount(amount);
		userAccountDetail.setOrderNo(orderNo);
		userAccountDetailMapper.insert(userAccountDetail);
	}
}
