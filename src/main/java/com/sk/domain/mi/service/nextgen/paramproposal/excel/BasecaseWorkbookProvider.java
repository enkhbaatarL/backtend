package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Basecase 원본 워크북 로더
 * 원본 xlsm 3개는 정적 리소스(classpath:basecase/)로 관리하며, 위치는 프로퍼티로 교체 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BasecaseWorkbookProvider {

    private final ResourceLoader resourceLoader;

    @Value("${mi.next-gen.model-input.basecase-location:classpath:basecase/}")
    private String basecaseLocation;

    XSSFWorkbook open(ModelInputFile file) {
        Resource resource = resourceLoader.getResource(locationOf(file));
        try (InputStream in = resource.getInputStream()) {
            return new XSSFWorkbook(in);
        } catch (IOException e) {
            log.error("[ModelInput Merge] basecase 원본 로드 실패: {}", locationOf(file), e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "수급모델 입력 병합 실패 — Basecase 원본을 열 수 없습니다: %s".formatted(file.getBasecaseFileName()));
        }
    }

    private String locationOf(ModelInputFile file) {
        String location = basecaseLocation.endsWith("/") ? basecaseLocation : basecaseLocation + "/";
        return location + file.getBasecaseFileName();
    }

}
