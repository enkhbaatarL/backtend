package com.sk.domain.mi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "02. Signal Map API", description = "Signal Map API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/signal-map")
@RequiredArgsConstructor
public class MiSignalMapController {

}
