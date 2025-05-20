package com.sk.domain.mi.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.domain.mi.dto.response.dashboard.LatestDataRes;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDashboardService {
    private final GoodsDataRepository goodsDataRepository;
    private final ChartPredictSettingRepository chartPredictSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    /**
     * 데이터 존재 여부 조회
     * 
     * @param companyNo
     * @return boolean
     */
    public boolean getDataExists(int companyNo) {
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(() -> new EntityNotFoundException("CustomerCompany not found"));

        return chartPredictSettingRepository.existsByCustomerCompany(customerCompany);
    }

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

    /**
     * 대시보드 차트 데이터 조회
     * 
     * @param companyNo
     * @return TODO
     */
    public Boolean getChart(int companyNo) {

        return true;
    }
}
