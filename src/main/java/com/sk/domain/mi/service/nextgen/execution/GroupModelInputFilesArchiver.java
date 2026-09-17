package com.sk.domain.mi.service.nextgen.execution;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionInputFileRes;
import com.sk.domain.shared.dto.request.file.FileDownloadReq;
import com.sk.domain.shared.service.FileDownloadEncryptor;
import com.sk.domain.shared.service.FileService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 시나리오 파라미터 비교 팝업의 인풋 파일 일괄(zip) 조립기.
 * 실행 완료 시나리오별 폴더(표시순서_시나리오명) 아래에 role명 파일(demand/supply/trade + 원본 확장자)로 담는다.
 * 외부 클라이언트 전달 경계이므로 공통 단건 다운로드와 동일한 재암호화 정책을 zip entry 단위로 적용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupModelInputFilesArchiver {

    private static final String DEFAULT_EXTENSION = "xlsm";
    private static final int FOLDER_NAME_MAX_LENGTH = 80;

    private final FileService fileService;
    private final FileDownloadEncryptor fileDownloadEncryptor;

    public record ScenarioInputFiles(
        Integer displayOrder,
        String scenarioName,
        List<ModelExecutionInputFileRes> files
    ) {
    }

    public byte[] archive(List<ScenarioInputFiles> sources) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {

            for (ScenarioInputFiles source : sources) {
                String folderName = folderNameOf(source);
                for (ModelExecutionInputFileRes file : source.files()) {
                    String entryFileName = entryFileNameOf(file);
                    byte[] plainData = fileService.downloadFile(
                        new FileDownloadReq(file.fileNo(), file.fileNm(), file.fileSize())
                    ).fileData();
                    byte[] responseData = fileDownloadEncryptor.encryptForDownloadIfRequired(entryFileName, plainData);

                    zip.putNextEntry(new ZipEntry("%s/%s".formatted(folderName, entryFileName)));
                    zip.write(responseData);
                    zip.closeEntry();
                }
            }
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("[GroupModelInput Archive] zip 생성 실패", e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "인풋 파일 zip 생성에 실패했습니다.");
        }
    }

    /** 표시순서_시나리오명. 시나리오명 중복·특수문자로 인한 entry 충돌을 표시순서 접두로 막는다. */
    private String folderNameOf(ScenarioInputFiles source) {
        int order = source.displayOrder() == null ? 0 : source.displayOrder();
        return "%d_%s".formatted(order, sanitizeFileName(source.scenarioName()));
    }

    /** role명 소문자 + 원본 확장자 (예: demand.xlsm). */
    private String entryFileNameOf(ModelExecutionInputFileRes file) {
        return "%s.%s".formatted(file.fileRole().toLowerCase(Locale.ROOT), extensionOf(file.fileNm()));
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return DEFAULT_EXTENSION;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return DEFAULT_EXTENSION;
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /** zip entry에 쓸 수 없는 경로·예약 문자를 치환하고 길이를 제한한다. */
    public static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").strip();
        if (sanitized.isEmpty()) {
            return "unnamed";
        }
        return sanitized.length() <= FOLDER_NAME_MAX_LENGTH ? sanitized : sanitized.substring(0, FOLDER_NAME_MAX_LENGTH);
    }
}
