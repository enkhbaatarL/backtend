package com.sk.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sk.domain.shared.client.dto.AXMetaFileDto;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class JsonFileWriter {

    private final ObjectMapper objectMapper;

    public JsonFileWriter() {
        objectMapper = new ObjectMapper()
                // Java 8 날짜 타입을 ISO 문자열로 직렬화
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * DocumentDto 객체를 지정된 경로에 JSON 파일로 저장합니다.
     *
     * @param documentDto 저장할 DTO
     * @param filePath    저장할 파일 경로 (예: "output/document.json")
     * @throws IOException 파일 쓰기 실패 시 예외 발생
     */
    public void write(AXMetaFileDto dto, String filePath) throws IOException {
        File target = new File(filePath);
        // pretty print 옵션
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(target, dto);
    }
}
