package com.sk.domain.mi.dto.response.dashboard.history;

import java.time.LocalDate;

public record HistoryDataRes(
    LocalDate date,
    PriceEntry baseline,
    PriceEntry counterpart
) {

}
