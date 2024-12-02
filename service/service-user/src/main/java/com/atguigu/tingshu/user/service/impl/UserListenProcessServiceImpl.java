package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.user.service.UserListenProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
		return null;
	}
}
