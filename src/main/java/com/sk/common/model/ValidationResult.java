package com.sk.common.model;

import com.sk.common.exception.AppErrorException;

public record ValidationResult<T>(boolean isValid, T data, AppErrorException error) {

    public static <T> ValidationResult<T> success(T data) {
        return new ValidationResult<>(true, data, null);
    }

    public static <T> ValidationResult<T> failure(AppErrorException error) {
        return new ValidationResult<>(false, null, error);
    }

    public T getOrThrow() {
        if (!isValid) {
            throw error;
        }
        return data;
    }

    public void validOrThrow() {
        if (!isValid) {
            throw error;
        }
    }

}
