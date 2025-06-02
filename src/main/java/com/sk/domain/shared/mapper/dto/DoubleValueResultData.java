package com.sk.domain.shared.mapper.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class DoubleValueResultData {
    private LocalDate date;
    private BigDecimal value;
}
