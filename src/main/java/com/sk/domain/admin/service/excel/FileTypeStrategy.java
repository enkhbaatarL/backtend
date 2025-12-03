package com.sk.domain.admin.service.excel;

import java.io.IOException;
import java.util.List;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

import com.sk.domain.shared.entity.master.tcm.ComponentInfo;

/**
 * 엑셀 파싱 전략
 */
public interface FileTypeStrategy {
    List<Object[]> processFile(MultipartFile file, ComponentInfo componentInfo, Map<String, String> conditionMap, String criteriaDt, String category)
            throws IOException;

    void postAction(ComponentInfo componentInfo, List<Object[]> values);
}
