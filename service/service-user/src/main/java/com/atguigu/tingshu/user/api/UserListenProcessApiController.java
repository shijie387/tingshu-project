package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GGLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

	@Autowired
	private UserListenProcessService userListenProcessService;

	/**
	 * 查询当前用户指定声音播放进度
	 *
	 * @param trackId 声音ID
	 * @return 前端必须返回具体数值，返回null导致前端无法触发更新播放进度定时任务
	 */
	@GGLogin(required = false)
	@GetMapping("/userListenProcess/getTrackBreakSecond/{trackId}")
	public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId) {
		//1.获取当前用户ID
		Long userId = AuthContextHolder.getUserId();
		if (userId != null) {
			//2.根据用户ID+声音ID查询播放进度
			BigDecimal bigDecimal = userListenProcessService.getTrackBreakSecond(userId, trackId);
			return Result.ok(bigDecimal);
		}
		return Result.ok(new BigDecimal("0.00"));
	}

}

