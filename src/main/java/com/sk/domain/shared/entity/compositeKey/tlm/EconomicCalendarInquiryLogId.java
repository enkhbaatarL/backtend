package com.sk.domain.shared.entity.compositeKey.tlm;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EconomicCalendarInquiryLogId implements Serializable {
    private Integer userNo;
    private String economicCalendarCode;
    private LocalDateTime inquiryDateTime;
}
