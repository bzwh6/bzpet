package com.bzpet.common.exception;

import lombok.Data;
import lombok.Getter;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Getter
public class BadException extends CommonException {
    private final Integer code = 400;

    public BadException(Integer code, String message) {
        super(400, message);
    }

    public BadException(String message) {
        super(message);
    }

    public BadException(Throwable throwable, String messge) {
        super(throwable, messge);
    }
    public BadException(Integer code,Throwable throwable,String messge){
        super(400, throwable, messge);
    }

}
