package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GGLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;


    /**
     * 提供给小程序微信登录接口
     *
     * @param code 小程序集成SDK后获取临时票据（基于当前微信用户产生的）
     * @return
     */
    @Operation(summary = "提供给小程序微信登录接口")
    @GetMapping("/wxLogin/{code}")
    public Result<Map<String, String>> wxLogin(@PathVariable String code) {
        Map<String, String> map = userInfoService.wxLogin(code);
        return Result.ok(map);
    }

    @GGLogin
    @GetMapping("/getUserInfo")
    public Result<UserInfoVo> getUserInfo(){
        Long userId = AuthContextHolder.getUserId();
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }

    @GGLogin
    @PostMapping("/updateUser")
    public Result updateUser(@RequestBody UserInfoVo userInfoVo){
        //1.获取用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.更新用户信息
        userInfoService.updateUser(userId, userInfoVo);
        return Result.ok();
    }
}
