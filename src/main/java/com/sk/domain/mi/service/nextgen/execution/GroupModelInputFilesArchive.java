package com.sk.domain.mi.service.nextgen.execution;

/**
 * 그룹 인풋 파일 일괄 다운로드 산출물 — zip 파일명과 바이트.
 */
public record GroupModelInputFilesArchive(

    String fileName,

    byte[] content

) {
}
