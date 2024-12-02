package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;


	/**
	 * 根据用户ID查询用户/主播基本信息
	 * @param userId
	 * @return
	 */
	@Operation(summary = "根据用户ID查询用户/主播基本信息")
	@GetMapping("/userInfo/getUserInfoVo/{userId}")
	public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId){
		UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
		return Result.ok(userInfoVo);
	}

	/**
	 * 检查提交声音ID列表购买情况
	 *
	 * @param userId                    用户ID
	 * @param albumId                   专辑ID
	 * @param needCheckBuyStateTrackIds 待检查购买情况声音ID列表
	 * @return 提交待检查购买声音ID购买结果 {38679:1,38678:0}
	 */
	@Operation(summary = "检查提交声音ID列表购买情况")
	@PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
	public Result<Map<Long, Integer>> userIsPaidTrack(
			@PathVariable Long userId,
			@PathVariable Long albumId,
			@RequestBody List<Long> needCheckBuyStateTrackIds
	) {
		Map<Long, Integer> map = userInfoService.userIsPaidTrack(userId, albumId, needCheckBuyStateTrackIds);
		return Result.ok(map);
	}

}

