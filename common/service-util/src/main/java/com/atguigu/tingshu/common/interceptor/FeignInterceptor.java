package com.atguigu.tingshu.common.interceptor;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * feign拦截器:拦截OpenFeign请求，添加token将上游服务的token传递给下游服务
 * 默认情况：A通过OpenFeign调用B，A中请求头中token不会传递到下游服务
 */

@Component
public class FeignInterceptor implements RequestInterceptor {

    public void apply(RequestTemplate requestTemplate){
        //  获取请求对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //异步编排 与 MQ消费者端 为 null
        if(null != requestAttributes) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes)requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String token = request.getHeader("token");
            requestTemplate.header("token", token);
        }
    }
}
