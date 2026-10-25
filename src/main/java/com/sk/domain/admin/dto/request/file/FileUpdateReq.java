package com.sk.domain.admin.dto.request.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

// @formatter:off
public record FileUpdateReq(
    @Schema(description = "파일관계객체식별번호", example = "1")
    Integer fileRelationshipObjectDistinguishNo,

    @Schema(description = "파일관계객체구분코드. 영문, 숫자, 밑줄, 공백만 허용하며 Case 원본 관계로 연결할 수 없습니다.", example = "BBS")
    @Pattern(regexp = "[A-Za-z0-9_ ]*")
    String fileRelationshipObjectSeparateCode, 

    @Schema(description = "파일관계객체구분코드", example = "99991231")
    String fileValidEndDt) {

      @Builder
      public  FileUpdateReq(Integer fileRelationshipObjectDistinguishNo, String fileRelationshipObjectSeparateCode, String fileValidEndDt) {
        this.fileRelationshipObjectDistinguishNo = fileRelationshipObjectDistinguishNo;
        this.fileRelationshipObjectSeparateCode = fileRelationshipObjectSeparateCode;
        this.fileValidEndDt = fileValidEndDt;
    }  

    public static FileUpdateReq of(Integer fileRelationshipObjectDistinguishNo, String fileRelationshipObjectSeparateCode, String fileValidEndDt) {
        return FileUpdateReq.builder()
                .fileRelationshipObjectDistinguishNo(fileRelationshipObjectDistinguishNo)
                .fileRelationshipObjectSeparateCode(fileRelationshipObjectSeparateCode)
                .fileValidEndDt(fileValidEndDt)
                .build();
    }
}
// @formatter:on