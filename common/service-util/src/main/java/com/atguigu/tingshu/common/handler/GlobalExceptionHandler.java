package com.atguigu.tingshu.common.handler;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 全局异常处理类：对Controller层进行增强（异常通知）目标方法发生异常时，自动调用此方法
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result error(Exception e) {
        log.error("触发Exception异常拦截:{}", e);
        return Result.fail();
    }

    /**
     * 自定义异常处理方法
     *
     * @param e
     * @return
     */
    @ExceptionHandler(GuiguException.class)
    public Result error(GuiguException e) {
        log.error("触发GuiguException异常拦截:{}", e);
        return Result.build(null, e.getCode(), e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public Result llegalArgumentException(Exception e) {
        log.error("触发异常拦截: " + e.getMessage(), e);
        return Result.build(null, ResultCodeEnum.ARGUMENT_VALID_ERROR);
    }


    @ExceptionHandler(value = BindException.class)
    public Result error(BindException exception) {
        BindingResult result = exception.getBindingResult();
        Map<String, Object> errorMap = new HashMap<>();
        List<FieldError> fieldErrors = result.getFieldErrors();
        fieldErrors.forEach(error -> {
            log.error("field: " + error.getField() + ", msg:" + error.getDefaultMessage());
            errorMap.put(error.getField(), error.getDefaultMessage());
        });

        log.error("触发BindException异常拦截: {}" + errorMap);
        return Result.build(errorMap, ResultCodeEnum.ARGUMENT_VALID_ERROR);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result error(MethodArgumentNotValidException exception) {
        BindingResult result = exception.getBindingResult();
        Map<String, Object> errorMap = new HashMap<>();
        List<FieldError> fieldErrors = result.getFieldErrors();
        fieldErrors.forEach(error -> {
            log.error("field: " + error.getField() + ", msg:" + error.getDefaultMessage());
            errorMap.put(error.getField(), error.getDefaultMessage());
        });
        log.error("触发MethodArgumentNotValidException异常拦截: {}" + errorMap);
        return Result.build(errorMap, ResultCodeEnum.ARGUMENT_VALID_ERROR);
    }
}
