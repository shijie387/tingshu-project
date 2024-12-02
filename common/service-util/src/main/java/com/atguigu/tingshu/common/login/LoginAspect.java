package com.atguigu.tingshu.common.login;


import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;

@Slf4j
@Component
@Aspect
public class LoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(ggLogin)")
    public Object doBasicProfiling(ProceedingJoinPoint pjp, GGLogin ggLogin) throws Throwable {

        //1.前置逻辑
        log.info("[认证注解切面]前置逻辑...");
        //1.1 获取请求对象；获取请求头名称为token的用户令牌
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes)requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();

        String token = request.getHeader("token");

        //1.2 将token作为Key查询Redis中用户信息
        //1.2.1 构建登录成功存入Redis用户令牌Key查询 形式：user:login:token值
        //1.2.2 获取存在Redis中用户信息UserInfoVo
        String key = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        UserInfoVo userInfoVo = (UserInfoVo)redisTemplate.opsForValue().get(key);

        //1.3 如果认证注解对象属性required=true且用户信息为空抛出异常：状态码=208 引导用户登录
        //1.4 如果用户信息有值，将用户ID存入ThreadLocal中
        if (ggLogin.required() == true && userInfoVo == null) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        if(userInfoVo != null) {
            AuthContextHolder.setUserId(userInfoVo.getId());
        }

        //2.执行目标方法(controller->service-mapper)
        Object retVal = pjp.proceed();
        // stop stopwatch

        //3.后置逻辑
        log.info("[认证注解切面]后置逻辑...");
        //3.1 手动清理ThreadLocal避免出现使用不当导致内存泄漏，不断内存泄漏导致内存溢出
        AuthContextHolder.removeUserId();

        return retVal;
    }
}
