package com.sk.domain.mi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorRes {
    private int status;
    private String error;
    private String message;
} 