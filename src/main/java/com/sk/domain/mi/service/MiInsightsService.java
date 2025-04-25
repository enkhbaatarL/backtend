package com.sk.domain.mi.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import com.sk.domain.mi.dto.response.insights.FocusedDataRes;
import com.sk.domain.mi.dto.response.insights.GoodsDataFilterRes;
import com.sk.domain.mi.dto.response.insights.IFocusedDataRes;
import com.sk.domain.mi.dto.response.insights.InsightsDataRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;
import com.sk.domain.shared.enums.GoodsDataManagementSeparateCode;
import com.sk.domain.shared.enums.insights.InsightsPeriodType;
import com.sk.domain.shared.enums.insights.InsightsSortType;
import com.sk.domain.shared.repository.master.tcm.CommonCodeDetailRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiInsightsService {
    private final CommonCodeDetailRepository commonCodeDetailRepository;
    private final GoodsDataRepository goodsDataRepository;
    private final EntityManager entityManager;
    private final GoodsRepository goodsRepository;
    private final GoodsDataCategoryRepository goodsDataCategoryRepository;

    /**
     * Focused 목록 조회
     *
     * @param companyNo
     * @return List<GoodsDataRes>
     */
    public List<FocusedDataRes> getFocused(int companyNo) {
        // LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        List<IFocusedDataRes> list = goodsDataRepository.findFocusedData(companyNo, PageRequest.of(0, 100));
        list.stream().forEach(d -> {
            System.out.println("FocusedDataRes : " + d.getGoodsDataNm());
            System.out.println("FocusedDataRes : " + d.getGoodsDataCategoryCd());
        });

        return null;
    }

    /**
     * Insights 고급 필터 목록 조회
     *
     * @return List<GoodsDataFilterRes>
     */
    public GoodsDataFilterRes getAdvancedFilter() {

        List<GoodsDataCategoryRes> documentTypes = commonCodeDetailRepository.findDocTypeCategory();
        List<GoodsDataCategoryRes> providers = commonCodeDetailRepository.findProviderTypeCategory();
        List<GoodsDataCategoryRes> languages = commonCodeDetailRepository.findLangTypeCategory();

        return GoodsDataFilterRes.of(documentTypes, providers, languages);
    }

    /**
     * Insights 목록 조회
     *
     * @param companyNo 고객사 번호
     * @param page      페이지 번호
     * @param size      페이지 사이즈
     * @param keyword   검색어
     * @param sort     정렬 타입
     * @param period    기간 타입
     * @param filter 고급 필터
     * @return PaginationRes<InsightsDataRes>
     */
    public PaginationRes<InsightsDataRes> getInsights(
            int companyNo,
            int page,
            int size,
            String keyword,
            InsightsSortType sort,
            InsightsPeriodType period,
            String filters) {

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
                .append(" AND tgd.del_yn = 'N'")
                .append(" AND tgdc.del_yn = 'N'");

        if (keyword != null && !keyword.isBlank()) {
            if (sort.equals(InsightsSortType.MOST_RELEVANT)) {
                // select.append(", MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN NATURAL LANGUAGE MODE) AS score");
                where.append(" AND MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN NATURAL LANGUAGE MODE)");
            } else {
                // select.append(", MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN BOOLEAN MODE) AS score");
                where.append(" AND MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN BOOLEAN MODE)");
            }
        }

        if (filters != null && !filters.isBlank()) {
            where.append(" AND tgdc.gds_data_ctgr_cd IN (:filters)");
        }

        switch (period) {
            case ONE_DAY -> where.append(" AND tgd.mod_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)");
            case ONE_WEEK -> where.append(" AND tgd.mod_dtm >= DATE_SUB(NOW(), INTERVAL 1 WEEK)");
            case ONE_MONTH -> where.append(" AND tgd.mod_dtm >= DATE_SUB(NOW(), INTERVAL 1 MONTH)");
            case THREE_MONTH -> where.append(" AND tgd.mod_dtm >= DATE_SUB(NOW(), INTERVAL 3 MONTH)");
            case SIX_MONTH -> where.append(" AND tgd.mod_dtm >= DATE_SUB(NOW(), INTERVAL 6 MONTH)");
            case TWELVE_MONTH -> where.append(" AND tgd.mod_dtm >= DATE_SUB(NOW(), INTERVAL 12 MONTH)");
            case ALL_DATES -> {
            } // no filter
        }

        String groupBy = " GROUP BY tg.prvd_no, tc.gds_no, tgd.gds_data_no ";

        String orderBy = switch (sort) {
            case NEWEST -> " ORDER BY tgd.mod_dtm DESC ";
            case OLDEST -> " ORDER BY tgd.mod_dtm ASC ";
            case MOST_RELEVANT -> " ";
        };

        select.append(" ");
        where.append(" ");

        String contentSql = """
                SELECT
                  """ + select + """
                FROM midb.tam_custcpnygdsmappl tc
                LEFT JOIN midb.tmd_gdsm tg ON tg.gds_no = tc.gds_no
                LEFT JOIN midb.tmd_prvdm tp on tp.prvd_no = tg.prvd_no
                LEFT JOIN midb.tmd_gdsdatam tgd ON tgd.gds_no = tg.gds_no
                LEFT JOIN midb.tmd_gdsdatactgrl tgdc ON tgdc.gds_data_no = tgd.gds_data_no
                JOIN (
                    SELECT DISTINCT gds_data_no
                    FROM midb.tmd_gdsdatactgrl
                    WHERE del_yn = 'N'
                    AND gds_data_mnagt_spt_cd = 'GUDAT'
                ) gud ON gud.gds_data_no = tgd.gds_data_no
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
                LEFT JOIN midb.tmd_gdsdatactgrl tgdc ON tgdc.gds_data_no = tgd.gds_data_no
                JOIN (
                    SELECT DISTINCT gds_data_no
                    FROM midb.tmd_gdsdatactgrl
                    WHERE del_yn = 'N'
                    AND gds_data_mnagt_spt_cd = 'GUDAT'
                ) gud ON gud.gds_data_no = tgd.gds_data_no
                WHERE
                """ + where;

        Query contentQuery = entityManager.createNativeQuery(contentSql);
        Query countQuery = entityManager.createNativeQuery(countSql);

        contentQuery.setParameter("companyNo", companyNo);
        countQuery.setParameter("companyNo", companyNo);

        if (keyword != null && !keyword.isBlank()) {
            if (sort.equals(InsightsSortType.MOST_RELEVANT)) {
                contentQuery.setParameter("keyword", keyword);
                countQuery.setParameter("keyword", keyword);
            } else {
                contentQuery.setParameter("keyword", String.format("+(%s)", keyword.replaceAll("(\\S+)", "$1*")));
                countQuery.setParameter("keyword", String.format("+(%s)", keyword.replaceAll("(\\S+)", "$1*")));
            }
        }

        if (filters != null && !filters.isBlank()) {
            List<String> filterList = Arrays.stream(filters.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .filter(s -> !s.isEmpty())
                    .toList();
            contentQuery.setParameter("filters", filterList);
            countQuery.setParameter("filters", filterList);
        }

        contentQuery.setFirstResult((page - 1) * size);
        contentQuery.setMaxResults(size);

        List<Object[]> rows = contentQuery.getResultList();
        List<InsightsDataRes> content = rows.stream()
                .map(row -> new InsightsDataRes(
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
     * 
     * debug용 더미 데이터 생성
     *
     */
    @Transactional
    public void createDummyData() {
        List<String> A_TYPES = List.of(
                "A_INSIGHT",
                "A_CMMDT_MRKT_RPT",
                "A_CMPY_RPT",
                "A_ASST_RPT",
                "A_DEAL_INSIGHT",
                "A_INFORM");

        List<String> A_LANGUAGES = List.of(
                "A_ENG",
                "A_KOR",
                "A_OTHERS");

        for (int i = 0; i < 100; i++) {
            Goods goods = goodsRepository.findByGoodsNo(5000, 70008).orElse(null);

            GoodsData savedGoodsData = goodsDataRepository
                    .save(GoodsData.of(goods, null, "데이타명 %d".formatted(i), "데이타설명 %d".formatted(i), "데이타요약 %d".formatted(i)));

            goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, "A_LSEG", "",
                    GoodsDataManagementSeparateCode.GDAT.getCode(), null));

            List<String> copy1 = new ArrayList<>(A_TYPES);
            Collections.shuffle(copy1);
            List<String> results1 = copy1.subList(0, 3);
            for (String result : results1) {
                goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, result, "",
                        GoodsDataManagementSeparateCode.GDAT.getCode(), null));
            }

            List<String> copy2 = new ArrayList<>(A_LANGUAGES);
            Collections.shuffle(copy2);
            List<String> results2 = copy2.subList(0, 2);
            for (String result : results2) {
                goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, result, "",
                        GoodsDataManagementSeparateCode.GDAT.getCode(), null));
            }

            goodsDataCategoryRepository.save(GoodsDataCategory.of(savedGoodsData, "PUBDATE", "20250425",
                    GoodsDataManagementSeparateCode.GUDAT.getCode(), 33L));

        }

    }
}
