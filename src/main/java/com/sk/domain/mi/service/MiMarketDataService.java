package com.sk.domain.mi.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataDetailReq;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.marketData.DataCatalogRes;
import com.sk.domain.mi.dto.response.marketData.MiMarketDataRes;
import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.shared.entity.master.tcm.MarketDataChartSetting;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tcm.MarketDataChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiMarketDataService {

    private static final int DATE_COLUMN_INDEX = 0; // 날짜 컬럼 인덱스
    private static final String INDEX_START_DATE_LIMIT = "1";
    private static final String MARKET_DATA_INTERVAL = "1Y";

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final MiGoodsDataInqueryUserService miGoodsDataInqueryUserService;
    private final MarketDataChartSettingRepository marketDataChartSettingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final MiUserService miUserService;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;

    public List<MiMarketDataRes> getMarketDatas(Integer customerCompanyNo, User user) {
        String START_DATE = DateUtil.toYmd(LocalDate.now().minusYears(1)); // 1년 전

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



        // DataSourceGroupMapping  => 비교군 데이터소스 조회 후에 같이 그려줘야 한다



        List<MiMarketDataRes> miMarketDataResponses = new ArrayList<>();

        for (MarketDataChartSetting marketDataChartSetting : marketDataChartSettings) {
            GoodsDataSourceDetail goodsDataSourceDetail = marketDataChartSetting.getGoodsDataSourceDetail();
            GoodsData goodsData = goodsDataSourceDetail.getGoodsData();




            // Boolean isPermitted = permissionRes.getPermittedGoods().contains(goodsData.getGoodsDataNo());

            // if (!isPermitted) {
            //     miMarketDataResponses.add(MiMarketDataRes.of(isPermitted));
            //     continue;
            // }



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



            // YOY, MOM, WOW 계산 



            // 
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

    /**
     * Market Data - Data Catalog 목록 조회
     *
     * @param companyNo 고객사 번호
     * @param page      페이지 번호
     * @param size      페이지 사이즈
     * @param keyword   검색어
     * @return PaginationRes<DataCatalogRes>
     */
    public PaginationRes<DataCatalogRes> getDataCatalog(
            int companyNo,
            int page,
            int size,
            String keyword) {

        Pageable pageable = PageRequest.of(page - 1, size);

        StringBuilder select = new StringBuilder()
                .append(" tp.prvd_no AS providerNo,")
                .append(" tp.prvd_nm AS providerNm,")
                .append(" tg.gds_no AS goodsNo,")
                .append(" tg.gds_nm AS goodsNm,")
                .append(" tgd.gds_data_no AS goodsDataNo,")
                .append(" tgd.gds_data_nm AS goodsDataNm,")
                .append(" tgd.gds_data_expn AS goodsDataExpn,")
                .append(" tgd.mod_dtm AS modifyDt");

        StringBuilder where = new StringBuilder()
                .append(" tc.cust_cpny_no = :companyNo")
                .append(" AND tc.del_yn = 'N'")
                .append(" AND tc.use_yn = 'Y'")
                .append(" AND tg.del_yn = 'N'")
                .append(" AND tg.use_yn = 'Y'")
                .append(" AND tp.del_yn = 'N'")
                .append(" AND tp.use_yn = 'Y'")
                .append(" AND tgd.del_yn = 'N'");

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN BOOLEAN MODE)");
        }

        String groupBy = " GROUP BY tg.prvd_no, tc.gds_no, tgd.gds_data_no ";

        String orderBy = " ORDER BY tgd.mod_dtm DESC ";

        select.append(" ");
        where.append(" ");

        String contentSql = """
                SELECT
                  """ + select + """
                FROM midb.tam_custcpnygdsmappl tc
                LEFT JOIN midb.tmd_gdsm tg ON tg.gds_no = tc.gds_no
                LEFT JOIN midb.tmd_prvdm tp on tp.prvd_no = tg.prvd_no
                LEFT JOIN midb.tmd_gdsdatam tgd ON tgd.gds_no = tg.gds_no
                WHERE
                """ + where + """
                """ + groupBy + """
                """ + orderBy;

        String countSql = """
                SELECT COUNT(DISTINCT tgd.gds_data_no) AS totalCount
                FROM midb.tam_custcpnygdsmappl tc
                LEFT JOIN midb.tmd_gdsm tg ON tg.gds_no = tc.gds_no
                LEFT JOIN midb.tmd_prvdm tp on tp.prvd_no = tg.prvd_no
                LEFT JOIN midb.tmd_gdsdatam tgd ON tgd.gds_no = tg.gds_no
                WHERE
                """ + where;

        Query contentQuery = entityManager.createNativeQuery(contentSql);
        Query countQuery = entityManager.createNativeQuery(countSql);

        contentQuery.setParameter("companyNo", companyNo);
        countQuery.setParameter("companyNo", companyNo);

        if (keyword != null && !keyword.isBlank()) {
            contentQuery.setParameter("keyword", String.format("+(%s)", keyword.replaceAll("(\\S+)", "$1*")));
            countQuery.setParameter("keyword", String.format("+(%s)", keyword.replaceAll("(\\S+)", "$1*")));
        }

        contentQuery.setFirstResult((page - 1) * size);
        contentQuery.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<DataCatalogRes> content = rows.stream()
                .map(row -> new DataCatalogRes(
                        (Integer) row[0], // providerNo
                        (String) row[1], // providerNm
                        (Integer) row[2], // goodsNo
                        (String) row[3], // goodsNm
                        (Integer) row[4], // goodsDataNo
                        (String) row[5], // goodsDataNm
                        (String) row[6], // goodsDataExpn
                        ((Timestamp) row[7]).toLocalDateTime() // modifyDt
                ))
                .collect(Collectors.toList());
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return PaginationRes.toRes(PageableExecutionUtils.getPage(content, pageable, () -> total));
    }

    /**
     * Market Data - Recent Viewed 최신 5개 조회
     *
     * @param userNo 사용자 번호
     * @return List<RecentViewedRes>
     */
    public List<RecentViewedRes> getRecentViewed(int userNo) {
        User user = userRepository.findByUserNoAndIsDeletedFalse(userNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "사용자 정보를 찾을 수 없습니다."));

        return miGoodsDataInqueryUserService.getGoodsDataInqueryUser(user.getUserLoginId(), PageRequest.of(0, 5));
    }
}
