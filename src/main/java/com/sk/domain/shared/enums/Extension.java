package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Extension {

    PDF("pdf"),
    DOC("doc"),
    DOCX("docx"),
    TXT("txt"),
    JPG("jpg"),
    JPEG("jpeg"),
    PNG("png"),
    GIF("gif"),
    MP3("mp3"),
    MP4("mp4"),
    XLS("xls"),
    XLSX("xlsx"),
    CSV("csv"),
    ZIP("zip");

    private final String code;

    /**
     * PDF 파일이면 PDF 뷰어로 해당 내용을 미리보기 할 수 있다.
     *
     * @param fileExtNm 확장자 명
     * @return Boolean
     */
    public static Boolean isPdfViewable(String fileExtNm) {
        return Extension.PDF.getCode().equalsIgnoreCase(fileExtNm);
    }

}
