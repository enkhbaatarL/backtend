package com.sk.domain.mi.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.insights.FocusedDataCache;
import com.sk.domain.mi.dto.response.insights.FocusedDataDto;
import com.sk.domain.mi.dto.response.insights.FocusedDataRes;
import com.sk.domain.mi.dto.response.insights.GoodsDataFilterRes;
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
import com.sk.domain.shared.service.permissions.AuthorizationService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiInsightsService {
    private static final int DATA_CACHE_TTL = 1; // todo: 캐시 유효기간 1시간 설정, 기본값: 3600

    private final CommonCodeDetailRepository commonCodeDetailRepository;
    private final GoodsDataRepository goodsDataRepository;
    private final EntityManager entityManager;
    private final GoodsRepository goodsRepository;
    private final GoodsDataCategoryRepository goodsDataCategoryRepository;
    private final AuthorizationService authorizationService;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Focused 목록 조회
     * 
     * 최신 등록된 데이터 조회
     * 조회결과 000개 있음
     * a. 000개중 Insight: 5개 출력
     * b. 그외 아래 순서대로 최신순: 3개출력
     *  (A) Commodity Market Report
     *  (B) Company Report
     *  (C) Asset Report
     *  (D) Deal Insight
     *  (E) Inform
     * 8개 정렬은 최신 등록순
     * 
     * @param companyNo
     * @return List<FocusedDataRes>
     */
    public List<FocusedDataRes> getFocused(int companyNo) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        // 캐시에서 데이터 조회
        String cacheKey = "FocusedData::companyNo:" + companyNo + "::threshold:" + threshold;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            try {
                FocusedDataCache focusedDataCache = objectMapper.readValue(cachedData, FocusedDataCache.class);
                if (focusedDataCache.data() != null) {
                    return focusedDataCache.data();
                }
            } catch (Exception e) {
                log.error("Failed to parse cached data for key: {}", cacheKey, e);
            }
        }

        List<FocusedDataDto> dataList = goodsDataRepository.findFocusedData(companyNo, threshold);

        List<FocusedDataRes> result = new ArrayList<>();
        List<FocusedDataRes> mutableList = dataList.stream()
                .map(res -> new FocusedDataRes(
                        res.getProviderNo(), res.getProviderNm(),
                        res.getGoodsNo(), res.getGoodsNm(),
                        res.getGoodsDataNo(), res.getGoodsDataNm(),
                        res.getModifyDt(), res.getGoodsDataCategoryCd(), false))
                .collect(Collectors.toList());

        Map<String, Integer> quotas = new LinkedHashMap<>();

        List<GoodsDataCategoryRes> documentTypes = commonCodeDetailRepository.findDocTypeCategory();
        for (GoodsDataCategoryRes documentType : documentTypes) {
            if (documentType.codeId().equals("A_INSIGHT")) {
                quotas.put(documentType.codeId(), 5);
            } else {
                quotas.put(documentType.codeId(), 1);
            }
        }

        PermissionRes permissionRes = authorizationService.getPermissions();

        for (Map.Entry<String, Integer> entry : quotas.entrySet()) {
            String type = entry.getKey();
            int limit = entry.getValue();

            Iterator<FocusedDataRes> it = mutableList.iterator();
            int count = 0;

            while (it.hasNext() && count < limit && result.size() < 8) {
                FocusedDataRes data = it.next();
                if (data.getGoodsDataCategoryCd().contains(type)
                        && (data.getGoodsDataCategoryCd().contains("A_ENG")
                                || (!data.getGoodsDataCategoryCd().contains("A_ENG") && !data.getGoodsDataCategoryCd().contains("A_KOR")
                                        && !data.getGoodsDataCategoryCd().contains("A_OTHERS")))) {

                    boolean isViewable = permissionRes.getGoodsViewableMap(companyNo).getOrDefault(data.getGoodsNo(), Boolean.FALSE);
                    data.setIsViewable(isViewable);
                    data.setGoodsDataCategoryCd(type);

                    result.add(data);
                    it.remove();
                    count++;
                }
            }

            if (result.size() >= 8) {
                break;
            }
        }

        // 캐시에 저장
        try {
            String json = objectMapper.writeValueAsString(FocusedDataCache.of(result));
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(DATA_CACHE_TTL));
        } catch (Exception e) {
            log.error("Error processing focused data", e);
        }

        return result;
    }

    /**
     * Insights 고급 필터 목록 조회
     *
     * @return GoodsDataFilterRes
     */
    public GoodsDataFilterRes getAdvancedFilter(int companyNo) {

        List<GoodsDataCategoryRes> documentTypes = commonCodeDetailRepository.findDocTypeCategory();
        List<GoodsDataCategoryRes> providers = commonCodeDetailRepository.findProviderTypeCategory(companyNo);
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
            String docFilters,
            String provFilters,
            String langFilters) {

        Pageable pageable = PageRequest.of(page - 1, size);

        List<String> docFilterList = (docFilters != null) ? List.of(docFilters.split(",")) : new ArrayList<>();
        docFilterList = docFilterList.stream()
                .map(String::trim)
                .filter(f -> !f.isEmpty())
                .collect(Collectors.toList());

        List<String> provFilterList = (provFilters != null) ? List.of(provFilters.split(",")) : new ArrayList<>();
        provFilterList = provFilterList.stream()
                .map(String::trim)
                .filter(f -> !f.isEmpty())
                .collect(Collectors.toList());

        List<String> langFilterList = (langFilters != null) ? List.of(langFilters.split(",")) : new ArrayList<>();
        langFilterList = langFilterList.stream()
                .map(String::trim)
                .filter(f -> !f.isEmpty())
                .collect(Collectors.toList());

        List<String> mergedList = new ArrayList<>();
        mergedList.addAll(docFilterList);
        mergedList.addAll(provFilterList);
        mergedList.addAll(langFilterList);

        StringBuilder select = new StringBuilder()
                .append(" tp.prvd_no AS providerNo,")
                .append(" tp.prvd_nm AS providerNm,")
                .append(" tg.gds_no AS goodsNo,")
                .append(" tg.gds_nm AS goodsNm,")
                .append(" tgd.gds_data_no AS goodsDataNo,")
                .append(" tgd.gds_data_nm AS goodsDataNm,")
                .append(" tgd.gds_data_expn AS goodsDataExpn,")
                .append(" tgd.gds_data_smry_cntt AS goodsDataSummaryCntt,")
                .append(" tgd.mod_dtm AS modifyDt");

        StringBuilder where = new StringBuilder()
                .append(" tp.del_yn = 'N'")
                .append(" AND tp.use_yn = 'Y'")
                .append(" AND EXISTS (SELECT 1 FROM midb.tcm_filel tf WHERE tf.del_yn = 'N' AND tf.file_rel_obj_spt_cd = 'GUDAT' AND tf.file_rel_obj_dstg_no = tgd.gds_data_no)");

        if (keyword != null && !keyword.isBlank()) {
            if (sort.equals(InsightsSortType.MOST_RELEVANT)) {
                where.append(" AND MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN NATURAL LANGUAGE MODE)");
            } else {
                where.append(" AND MATCH(tgd.gds_data_nm) AGAINST(lower(:keyword) IN BOOLEAN MODE)");
            }
        }

        if (mergedList != null && !mergedList.isEmpty()) {
            StringBuilder exists = new StringBuilder()
                    .append(" AND EXISTS (")
                    .append("   SELECT 1")
                    .append("   FROM midb.tmd_gdsdatactgrl tgdc")
                    .append("   WHERE tgdc.gds_data_no = tgd.gds_data_no")
                    .append("     AND tgdc.gds_data_ctgr_cd IN (:mergedList)")
                    .append("   GROUP BY tgdc.gds_data_no")
                    .append("   HAVING 1=1");

            if (docFilterList != null && !docFilterList.isEmpty()) {
                exists.append(" AND SUM(tgdc.gds_data_ctgr_cd IN (:docFilterList)) > 0");
            }

            if (provFilterList != null && !provFilterList.isEmpty()) {
                exists.append(" AND SUM(tgdc.gds_data_ctgr_cd IN (:provFilterList)) > 0");
            }

            if (langFilterList != null && !langFilterList.isEmpty()) {
                exists.append(" AND SUM(tgdc.gds_data_ctgr_cd IN (:langFilterList)) > 0");
            }

            exists.append(" ) ");
            where.append(exists);
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

        // String groupBy = " GROUP BY tp.prvd_no, tg.gds_no, tgd.gds_data_no ";

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
                FROM midb.tmd_prvdm tp
                JOIN midb.tmd_gdsm tg
                        ON tg.prvd_no = tp.prvd_no
                        AND tg.del_yn = 'N'
                        AND tg.use_yn = 'Y'
                JOIN midb.tmd_gdsdatam tgd
                        ON tgd.gds_no = tg.gds_no
                        AND tgd.del_yn = 'N'
                LEFT JOIN midb.tam_custcpnyprvdmappl tcp
                		ON tcp.prvd_no = tp.prvd_no
                		AND tcp.cust_cpny_no = :companyNo
                		AND tcp.del_yn = 'N'
                        AND tcp.use_yn = 'Y'
                        AND tgd.gds_data_mark_yn  = 'Y'
                LEFT JOIN midb.tam_custcpnygdsmappl tcg
                        ON tcg.gds_no = tg.gds_no
                        AND tcg.cust_cpny_no = :companyNo
                        AND tcg.del_yn = 'N'
                        AND tcg.use_yn = 'Y'
                WHERE
                """ + where + """
                """ + orderBy;

        String countSql = """
                SELECT COUNT(DISTINCT tgd.gds_data_no) AS totalCount
                FROM midb.tmd_prvdm tp
                JOIN midb.tmd_gdsm tg
                        ON tg.prvd_no = tp.prvd_no
                        AND tg.del_yn = 'N'
                        AND tg.use_yn = 'Y'
                JOIN midb.tmd_gdsdatam tgd
                        ON tgd.gds_no = tg.gds_no
                        AND tgd.del_yn = 'N'
                LEFT JOIN midb.tam_custcpnyprvdmappl tcp
                		ON tcp.prvd_no = tp.prvd_no
                		AND tcp.cust_cpny_no = :companyNo
                		AND tcp.del_yn = 'N'
                        AND tcp.use_yn = 'Y'
                        AND tgd.gds_data_mark_yn  = 'Y'
                LEFT JOIN midb.tam_custcpnygdsmappl tcg
                        ON tcg.gds_no = tg.gds_no
                        AND tcg.cust_cpny_no = :companyNo
                        AND tcg.del_yn = 'N'
                        AND tcg.use_yn = 'Y'
                WHERE
                """
                + where;

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

        if (mergedList != null && !mergedList.isEmpty()) {
            contentQuery.setParameter("mergedList", mergedList);
            countQuery.setParameter("mergedList", mergedList);
        }

        if (docFilterList != null && !docFilterList.isEmpty()) {
            contentQuery.setParameter("docFilterList", docFilterList);
            countQuery.setParameter("docFilterList", docFilterList);
        }

        if (provFilterList != null && !provFilterList.isEmpty()) {
            contentQuery.setParameter("provFilterList", provFilterList);
            countQuery.setParameter("provFilterList", provFilterList);
        }

        if (langFilterList != null && !langFilterList.isEmpty()) {
            contentQuery.setParameter("langFilterList", langFilterList);
            countQuery.setParameter("langFilterList", langFilterList);
        }

        contentQuery.setFirstResult((page - 1) * size);
        contentQuery.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();

        PermissionRes permissionRes = authorizationService.getPermissions();

        List<InsightsDataRes> content = rows.stream()
                .map(row -> new InsightsDataRes(
                        (Integer) row[0], // providerNo
                        (String) row[1], // providerNm
                        (Integer) row[2], // goodsNo
                        (String) row[3], // goodsNm
                        (Integer) row[4], // goodsDataNo
                        (String) row[5], // goodsDataNm
                        (String) row[6], // goodsDataExpn
                        (String) row[7], // goodsDataSummaryCntt
                        getGoodsDataCategory((Integer) row[4]), // goodsDataCategory
                        ((Timestamp) row[8]).toLocalDateTime(), // modifyDt
                        permissionRes.getGoodsViewableMap(companyNo).getOrDefault((Integer) row[2], Boolean.FALSE))) // viewable
                .collect(Collectors.toList());
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return PaginationRes.toRes(PageableExecutionUtils.getPage(content, pageable, () -> total));
    }

    String getGoodsDataCategory(int goodsDataNo) {
        return goodsDataCategoryRepository.findGoodsDataCategoryByGoodsDataNo(goodsDataNo)
                .stream()
                .map(GoodsDataCategory::getGoodsDataCategoryCd)
                .distinct()
                .collect(Collectors.joining(","));
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
                    .save(GoodsData.of(goods, null, "데이타명 %d".formatted(i), "데이타설명 %d".formatted(i), "데이타요약 %d".formatted(i),
                            "기고자명 %d".formatted(i), false, true));

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
