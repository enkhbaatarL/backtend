package com.sk.common.model;

import com.sk.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class BaseResponse<T> {

    private boolean status;
    private String code;
    private String message;
    private T data;

    public BaseResponse(String code, String message) {
        this.status = ResultCode.isSuccess(code);
        this.code = code;
        this.message = message;
    }

    public BaseResponse(String code, String message, T data) {
        this.status = ResultCode.isSuccess(code);
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(ResultCode.Success result, String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

}
