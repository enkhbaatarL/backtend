package com.sk.domain.admin.dto.response.provider;

import com.sk.domain.admin.dto.request.provider.Recommendation;
import com.sk.domain.admin.dto.request.provider.UnstructuredDataLinkReq;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record GoodsDataExtensionPayloadRes(
    @Schema(description = "상품데이터번호")
    Integer goodsDataNo,

    @Schema(description = "작성자")
    String authors,

    @Schema(description = "추천 리스트")
    List<Recommendation> recommendations,

    @Schema(description = "URL")
    String url,

    @Schema(description = "제공자 콘텐츠 ID")
    String providerContentId,

    @Schema(description = "비정형 데이터 링크 리스트")
    List<UnstructuredDataLinkReq> unstructuredDataLinkList
) {
}
