package com.sk.domain.shared.repository.master.tif.custom;

import java.time.LocalDateTime;
import java.util.List;

import com.sk.domain.shared.dto.response.rpa.CrackSpreadDataRes;

public interface CustomFuturesSpreadRepository {
    LocalDateTime findLastDateByCode(String code);

    List<CrackSpreadDataRes> calculateCrackSpreadData(LocalDateTime lastDate);
}
