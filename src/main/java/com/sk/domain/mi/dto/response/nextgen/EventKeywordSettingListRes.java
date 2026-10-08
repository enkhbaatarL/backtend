package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record EventKeywordSettingListRes(

    @Schema(description = "등록된 키워드 수", example = "3")
    int keywordCount,

    @Schema(description = "등록 가능한 최대 키워드 수", example = "20")
    int maxKeywordCount,

    @Schema(description = "등록된 키워드 목록 (등록순)")
    List<EventKeywordSettingRes> keywords

) {

    public static EventKeywordSettingListRes of(List<EventKeywordSettingRes> keywords, int maxKeywordCount) {
        return new EventKeywordSettingListRes(keywords.size(), maxKeywordCount, keywords);
    }

}
