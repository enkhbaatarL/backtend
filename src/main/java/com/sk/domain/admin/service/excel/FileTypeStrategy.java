package com.sk.domain.admin.service.excel;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sk.domain.shared.entity.master.tcm.ComponentInfo;

/**
 * 엑셀 파싱 전략
 */
public interface FileTypeStrategy {
    List<Object[]> processFile(MultipartFile file, ComponentInfo componentInfo, int headerRowIndex, int dataStartRowIndex, String criteriaDt, String category)
            throws IOException;
}
