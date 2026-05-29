package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDate;

public record EventOutlookRow(

    LocalDate viewDate,
    String institutionName,
    String outlookSummary

) {

    @QueryProjection
    public EventOutlookRow {
    }

}
