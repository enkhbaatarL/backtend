package com.sk.common.model;

import com.sk.common.enums.ResultCode;

public class BaseResponseFactory {

    private static final BaseResponseFactory INSTANCE = new BaseResponseFactory();

    public static <T> BaseResponse<T> create(T t) {
        return INSTANCE.internalCreate(t);
    }

    public static <T> BaseResponse<T> create(ResultCode.Success successEnum) {
        return INSTANCE.internalCreate(successEnum);
    }

    public static <T> BaseResponse<T> create(ResultCode.Success successEnum, T data) {
        return INSTANCE.internalCreate(successEnum, data);
    }

    public static <T> BaseResponse<T> create(ResultCode.Error errorEnum) {
        return INSTANCE.internalCreate(errorEnum);
    }

    public static <T> BaseResponse<T> create(ResultCode.Error errorEnum, T data) {
        return INSTANCE.internalCreate(errorEnum, data);
    }

    public static <T> BaseResponse<T> create(String code, String message) {
        return INSTANCE.internalCreate(code, message);
    }

    public static <T> BaseResponse<T> create(String code, String message, T data) {
        return INSTANCE.internalCreate(code, message, data);
    }

    public static <T> BaseResponse<T> createDetail(ResultCode.Error errorEnum, String causeStr) {
        return INSTANCE.internalCreate(errorEnum.getCode(), errorEnum.getMessage() + "(" + causeStr + ")");
    }

    private <T> BaseResponse<T> internalCreate(T t) {
        return new BaseResponse<>(ResultCode.Success.OK.getCode(), ResultCode.Success.OK.getMessage(), t);
    }

    private <T> BaseResponse<T> internalCreate(ResultCode.Success resultCode) {
        return new BaseResponse<>(resultCode.getCode(), resultCode.getMessage());
    }

    private <T> BaseResponse<T> internalCreate(ResultCode.Success resultCode, T data) {
        return new BaseResponse<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    private <T> BaseResponse<T> internalCreate(ResultCode.Error resultCode) {
        return new BaseResponse<>(resultCode.getCode(), resultCode.getMessage());
    }

    private <T> BaseResponse<T> internalCreate(ResultCode.Error resultCode, T data) {
        return new BaseResponse<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    private <T> BaseResponse<T> internalCreate(String code, String message) {
        return new BaseResponse<>(code, message);
    }

    private <T> BaseResponse<T> internalCreate(String code, String message, T data) {
        return new BaseResponse<>(code, message, data);
    }

}
