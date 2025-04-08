package com.sk.domain.mi.service;

import com.sk.domain.mi.dto.response.PermissionRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiDataDetailService {

    public PermissionRes getGoodsDataCharts(Integer goodsDataNo) {
        // 1. 개인화 차트 설정 내역 조회
        // 2. 상품 데이터 소스 상세 조인(차트)
        // 3. 데이터 소스 그룹 매핑 조인(차트 비교군)
        // 4. 단위 변환 및 indicator 추가
        return null;
    }

}
