package com.bzpet.common.exception;

import lombok.Getter;

@Getter
public class CommonException extends RuntimeException{
    private Integer code;

    public CommonException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    public CommonException(String message) {
        super(message);
        this.code = 500;
    }
    public CommonException(Throwable throwable,String messge){
        super(messge, throwable);
        this.code = 500;
    }
    public CommonException(Integer code,Throwable throwable,String messge){
        super(messge, throwable);
        this.code = code;
    }
}
