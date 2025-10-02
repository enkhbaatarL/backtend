package com.sk.domain.admin.service.collector.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// @formatter:off
public record SkEnsDataAType(
    LocalDate date,
    LocalDate strip,
    String contract,
    BigDecimal settlement
){



}
// @formatter:on
