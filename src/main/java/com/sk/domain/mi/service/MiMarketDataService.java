package com.sk.domain.mi.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.marketData.MiMarketDataRes;
import com.sk.domain.shared.entity.master.tcm.MarketDataChartSetting;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tcm.MarketDataChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiMarketDataService {

    private static final int DATE_COLUMN_INDEX = 0; // 날짜 컬럼 인덱스
    private static final String INDEX_START_DATE_LIMIT = "1";

    private final MarketDataChartSettingRepository marketDataChartSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final MiUserService miUserService;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    public List<MiMarketDataRes> getMarketDatas(Integer customerCompanyNo, User user) {
        String START_DATE = DateUtil.toYmd(LocalDate.now().minusMonths(1)); // 1달 전

        CustomerCompany customerCompany = customerCompanyRepository.findById(customerCompanyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("고객회사 조회 실패 : %s", customerCompanyNo)));

        // 유저별 권한 조회
        PermissionRes permissionRes;
        if (user.getUserRoleCd() == UserRoleCd.ADMIN) {
            // 관리자 권한
            permissionRes = miUserService.getAdminGoodsPermissions();
        } else if (user.getUserRoleCd() == UserRoleCd.USER) {
            // 사용자 권한
            permissionRes = miUserService.getUserGoodsPermissions(user.getUserNo());
        } else {
            throw AppErrorException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "사용자 권한 조회 실패");
        }

        List<MarketDataChartSetting> marketDataChartSettings = marketDataChartSettingRepository
                .findByCustomerCompanyAndIsDeletedFalseOrderByChartMarkRankAsc(customerCompany);

        List<MiMarketDataRes> miMarketDataResponses = new ArrayList<>();

        for (MarketDataChartSetting marketDataChartSetting : marketDataChartSettings) {
            GoodsDataSourceDetail goodsDataSourceDetail = marketDataChartSetting.getGoodsDataSourceDetail();
            GoodsData goodsData = goodsDataSourceDetail.getGoodsData();

            Boolean isPermitted = permissionRes.getPermittedGoods().contains(goodsData.getGoodsDataNo());

            if (!isPermitted) {
                miMarketDataResponses.add(MiMarketDataRes.of(isPermitted));
                continue;
            }
            
            // 날짜 칼럼
            String dateColumnName = goodsDataSourceDetail.getTableColumnListNm().split(",")[DATE_COLUMN_INDEX];
            // 원시 데이터 조회
            List<Map<String, Object>> rawData = getMarketDataRows(goodsDataSourceDetail, dateColumnName, START_DATE);
            
            // Index Start date 데이터 조회
            String indexStartDate;
            List<Map<String, Object>> indexStartDateData = getIndexStartDateData(goodsDataSourceDetail, dateColumnName);
            if (!indexStartDateData.isEmpty()) {
                indexStartDate = indexStartDateData.get(0).get(dateColumnName).toString();
            } else {
                indexStartDate = START_DATE;
            }
            
        }
        return miMarketDataResponses;
    }

    // Class 분리 시킬지 고민중

    /**
     * 날짜 범위를 기준으로 데이터베이스에서 데이터 행을 조회한다.
     * 데이터소스 상세 정보와 날짜 컬럼명, 시작일, 종료일을 기반으로 데이터를 필터링하여 조회한다.
     * 조건에 맞는 데이터가 없는 경우 빈 리스트를 반환한다.
     *
     * @param goodsDataSourceDetail 데이터소스 상세 정보
     * @param dateColumnName        필터링할 날짜 컬럼명
     * @param startDate             데이터 범위 시작일 (포함, YYYYMMDD 형식)
     * @return 조건에 맞는 데이터 행 목록
     */
    @Transactional
    public List<Map<String, Object>> getMarketDataRows(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName, String startDate) {

        if (goodsDataSourceDetail == null || dateColumnName == null || startDate == null) {
            return Collections.emptyList();
        }

        String whereClause = "%s >= '%s'".formatted(dateColumnName, startDate);

        return mybatisDynamicQueryService.getDynamicData(DynamicDataReq.of(goodsDataSourceDetail, whereClause));
    }

    /**
     * Index Start Date 데이터 조회
     */
    @Transactional
    public List<Map<String, Object>> getIndexStartDateData(GoodsDataSourceDetail goodsDataSourceDetail, String dateColumnName) {

        if (goodsDataSourceDetail == null || dateColumnName == null) {
            return Collections.emptyList();
        }

        return mybatisDynamicQueryService.getDynamicRawQuery(DynamicDataDetailReq.ofLimit(goodsDataSourceDetail.getTargetDbNm(),
                goodsDataSourceDetail.getTargetTableNm(), dateColumnName, goodsDataSourceDetail.getQueryConditionCntt(), INDEX_START_DATE_LIMIT));
    }
}
