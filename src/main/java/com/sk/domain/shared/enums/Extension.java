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

}
