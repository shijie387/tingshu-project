package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaUserInfo;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.login.GGLogin;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

	@Autowired
	private UserInfoMapper userInfoMapper;
	@Autowired
	private WxMaService wxMaService;
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private RabbitService rabbitService;
	@Autowired
	private UserPaidAlbumMapper userPaidAlbumMapper;
	@Autowired
	private UserPaidTrackMapper userPaidTrackMapper;

	@Override
	public Map<String, String> wxLogin(String code) {
		HashMap<String, String> map = null;
		try {
			//1.根据小程序提交code再加appid+appsecret获取微信账户唯一标识 wxOpenId
			WxMaUserService userService = wxMaService.getUserService();
			WxMaJscode2SessionResult sessionInfo = userService.getSessionInfo(code);
			String openid = sessionInfo.getOpenid();


			//2.根据微信唯一标识查询用户记录
			LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(UserInfo::getWxOpenId, openid);
			UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);

			//3.判断用户记录是否存在(用户是否绑定过该微信账号)
			if (userInfo == null) {
				//first-time login
				//TODO 如果不存在，新增用户记录绑定微信账户唯一标识，隐式初始化账户记录
				//3.1 构建保存用户信息记录绑定微信唯一标识
				userInfo = new UserInfo();
				userInfo.setNickname("ami" + IdUtil.getSnowflakeNextId());
				userInfo.setAvatarUrl("http://192.168.200.6:9000/tingshu/20241123/44ec7b68-2ad8-4fa3-8dd9-320b82f7b48c.png");
				userInfo.setWxOpenId(openid);

				userInfoMapper.insert(userInfo);

				//3.2 隐式初始化账户记录
				//3.2.1 构建初始化账户所需参数对象
				//3.2.2 调用发送消息工具类方法发送消息 rabbitMQ
				HashMap<String, Object> msgData = new HashMap<>();
				msgData.put("userId", userInfo.getId());
				msgData.put("title", "首次登录赠送体验金");
				msgData.put("amount", new BigDecimal("10"));
				msgData.put("orderNo", "zs"+IdUtil.getSnowflakeNextId());

				rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, msgData);
			}

			//4.为用户生成令牌存入Redis 其中Redis中Key=前缀+token Value=用户基本信息UserInfoVo
			//4.1 生成token值
			//4.2 构建用户登录Key 形式=user:login:token值
			//4.3 将用户登录信息UserInfoVo存入Redis

			//5.封装token到对象响应给前端
			String token = IdUtil.randomUUID();
			String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;

			UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
			redisTemplate.opsForValue().set(loginKey,userInfoVo,RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

			map = new HashMap<>();
			map.put("token", token);
		} catch (WxErrorException e) {
			log.error("wx login error", e);
			throw new RuntimeException(e);
		}

		return map;
	}

	@GGLogin
	@Override
	public UserInfoVo getUserInfo(Long userId) {
		UserInfo userInfo = userInfoMapper.selectById(userId);
		if (userInfo != null) {
			UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
			return userInfoVo;
		}
		return null;
	}

	@Override
	public void updateUser(Long userId, UserInfoVo userInfoVo) {
		UserInfo userInfo = new UserInfo();
		userInfo.setId(userId);
		userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
		userInfo.setNickname(userInfoVo.getNickname());
		userInfoMapper.updateById(userInfo);
	}

	@Override
	public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckBuyStateTrackIds) {
		Map<Long, Integer> map = new HashMap<>();
		//1.根据用户ID+专辑ID查询专辑购买记录
		LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
		userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getAlbumId, albumId);
		userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getUserId, userId);
		Long count = userPaidAlbumMapper.selectCount(userPaidAlbumLambdaQueryWrapper);
		//1.1 存在购买记录
		if (count > 0) {
			for (Long needCheckBuyStateTrackId : needCheckBuyStateTrackIds) {
				//1.1 将所有提交待检查声音ID购买情况设置1
				map.put(needCheckBuyStateTrackId, 1);
			}
			return map;
		}
		//2.根据用户ID+专辑ID查询已购声音表
		LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
		userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getUserId, userId);
		userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getAlbumId, albumId);
		userPaidTrackLambdaQueryWrapper.select(UserPaidTrack::getTrackId);
		List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(userPaidTrackLambdaQueryWrapper);
		if (CollectionUtil.isEmpty(userPaidTrackList)) {
			//2.1 如果不存在已购声音 将所有提交待检查声音ID购买情况设置0返回
			for (Long needCheckBuyStateTrackId : needCheckBuyStateTrackIds) {
				map.put(needCheckBuyStateTrackId, 0);
			}
			return map;
		}
		//2.2 存在已购声音，遍历待检查声音ID列表判断找出已购声音购买情况设置1，未购买声音购买情况设置为0
		List<Long> userPaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
		for (Long needCheckBuyStateTrackId : needCheckBuyStateTrackIds) {
			//判断已购声音ID列表中是否存在待检查声音ID
			if(userPaidTrackIdList.contains(needCheckBuyStateTrackId)){
				map.put(needCheckBuyStateTrackId, 1);
			}else{
				map.put(needCheckBuyStateTrackId, 0);
			}
		}
		return map;
	}
}
