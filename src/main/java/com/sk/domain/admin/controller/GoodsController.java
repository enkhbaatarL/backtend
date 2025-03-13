package com.sk.domain.admin.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "04. 데이타 프로바이더 상품 API", description = "데이타 프로바이더 상품 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/goods")
@RequiredArgsConstructor
public class GoodsController {

}
