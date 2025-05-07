package com.sk.domain.shared.repository.master.tif.custom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.sk.domain.shared.entity.master.tif.Futures;

public interface CustomFuturesRepository {
    Map<LocalDateTime, List<Futures>> findPendingByCodes(List<String> codes);
}
