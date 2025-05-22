package com.sk.domain.mi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDashboardHistoryService {

    public Object getForecastDate(Integer companyNo) {
        return null;
    }

}
