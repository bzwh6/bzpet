package com.bzpet.common.autoConfig.handler;

import com.bzpet.common.constants.ErrorInfo;
import com.bzpet.common.domain.R;
import com.bzpet.common.exception.BadException;
import com.bzpet.common.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBadException(BadException e){
        log.error("业务异常：状态码{},错误信息{}", e.getCode(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(","));
        log.error("方法参数校验失败：{}", message);
        return R.error(ErrorInfo.Code.FAILED, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e){
        String message = e.getMessage();
        log.error("服务器异常错误信息：{}", message);
        return R.error(ErrorInfo.Code.FAILED, ErrorInfo.Msg.SERVER_INTER_ERROR);
    }
}
