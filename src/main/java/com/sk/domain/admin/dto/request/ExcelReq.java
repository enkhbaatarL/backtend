package com.sk.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExcelReq {
  // @NotEmpty
  @Schema(description = "이름", example = "김인철")
  private String name;

  // @NotEmpty
  @Schema(description = "등급", example = "갈라이머")
  private String grade;
}
