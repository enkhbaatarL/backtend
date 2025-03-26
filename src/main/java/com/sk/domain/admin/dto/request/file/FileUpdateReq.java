package com.sk.domain.admin.dto.request.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record FileUpdateReq(
    @Schema(description = "파일관계객체식별번호", example = "1")
    Integer fileRelationshipObjectDistinguishNo,

    @Schema(description = "파일관계객체구분코드", example = "BBS")
    String fileRelationshipObjectSeparateCode, 

    @Schema(description = "파일관계객체구분코드", example = "99991231")
    String fileValidEndDt) {

    public static FileUpdateReq of(Integer fileRelationshipObjectDistinguishNo, String fileRelationshipObjectSeparateCode, String fileValidEndDt) {
        return FileUpdateReq.builder()
                .fileRelationshipObjectDistinguishNo(fileRelationshipObjectDistinguishNo)
                .fileRelationshipObjectSeparateCode(fileRelationshipObjectSeparateCode)
                .fileValidEndDt(fileValidEndDt)
                .build();
    }
}
// @formatter:on