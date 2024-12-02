package com.atguigu.tingshu.common.login;


import org.springframework.aot.hint.annotation.Reflective;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GGLogin {
    boolean required() default true;  //can access if login
}
