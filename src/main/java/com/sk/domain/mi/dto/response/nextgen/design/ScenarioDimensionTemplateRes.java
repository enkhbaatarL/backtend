package com.sk.domain.mi.dto.response.nextgen.design;

import java.util.List;

/**
 * 분기항목/분기기준 템플릿.
 * 단계1 분기항목 추가 및 분기기준값 선택 dropdown에 사용한다.
 */
public record ScenarioDimensionTemplateRes(
    List<Item> dimensions
) {

    /** 분기항목 1개와 선택 가능한 분기기준값 목록. */
    public record Item(
        String dimKey,
        String label,
        Boolean required,
        Integer displayOrder,
        List<String> valueEnums
    ) {}

}
