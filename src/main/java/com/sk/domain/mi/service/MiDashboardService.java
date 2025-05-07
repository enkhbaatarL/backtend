package com.sk.domain.mi.service;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.domain.mi.dto.response.dashboard.LatestDataRes;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDashboardService {
    private final GoodsDataRepository goodsDataRepository;

    /**
     * Latest Data 목록 조회
     * 
     * @param companyNo
     * @return List<LatestDataRes>
     */
    public List<LatestDataRes> getLatestData(int companyNo) {

        return goodsDataRepository.findLatestData(companyNo, PageRequest.of(0, 5));
    }

    /**
     * Latest Insights 목록 조회
     * 
     * @param companyNo
     * @return List<LatestInsights>
     */
    public List<LatestInsightsRes> getLatestInsights(int companyNo) {

        return goodsDataRepository.findLatestInsights(companyNo, PageRequest.of(0, 5));
    }
}
