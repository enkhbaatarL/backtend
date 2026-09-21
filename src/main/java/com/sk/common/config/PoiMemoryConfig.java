package com.sk.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.ZipPackage;
import org.apache.poi.openxml4j.util.ZipInputStreamZipEntrySource;
import org.springframework.context.annotation.Configuration;

/**
 * POI 전역 메모리 정책 — 대형 xlsx/xlsm을 다루는 모든 POI 경로에 적용된다.
 *
 * 기본 동작은 패키지 파트(압축 해제된 XML)를 힙에 통째로 올리는데, 파일이 크면 파트만으로 수백 MB를 차지한다.
 * 파트를 임시 파일로 내리고, 스트림 오픈 시 대형 entry도 디스크로 우회시켜 힙 상주량을 줄인다.
 */
@Slf4j
@Configuration
public class PoiMemoryConfig {

    private static final int TEMP_FILE_THRESHOLD_BYTES = 16 * 1024 * 1024;

    @PostConstruct
    void configure() {
        ZipPackage.setUseTempFilePackageParts(true);
        ZipInputStreamZipEntrySource.setThresholdBytesForTempFiles(TEMP_FILE_THRESHOLD_BYTES);
        log.info("POI memory policy applied. useTempFilePackageParts=true, streamTempFileThresholdBytes={}", TEMP_FILE_THRESHOLD_BYTES);
    }

}
