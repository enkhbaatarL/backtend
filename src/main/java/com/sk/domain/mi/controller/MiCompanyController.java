package com.sk.domain.mi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.MiCompanyRes;
import com.sk.domain.mi.service.MiCompanyService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "01. Company API", description = "Company API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/company-management")
@RequiredArgsConstructor
public class MiCompanyController {

    private final MiCompanyService miCompanyService;

    @GetMapping("/companies/all")
    public BaseResponse<List<MiCompanyRes>> getCompanyList() {
        return BaseResponseFactory.create(miCompanyService.getCompanyList());
    }
}