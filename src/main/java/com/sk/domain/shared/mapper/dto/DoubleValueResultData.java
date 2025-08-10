package com.sk.domain.shared.mapper.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DoubleValueResultData {
    private LocalDateTime date;
    private BigDecimal value;
}
