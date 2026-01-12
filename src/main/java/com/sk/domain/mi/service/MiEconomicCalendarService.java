package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarDetailRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarHistoryRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarWidgetRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarWidgetRes.EconomicCalendarWidget;
import com.sk.domain.mi.dto.response.economiccalendar.KeyEconomicIndicatorsRes;
import com.sk.domain.mi.dto.response.economiccalendar.KeyEconomicIndicatorsRes.RelatedEconomicIndicator;
import com.sk.domain.mi.dto.response.economiccalendar.RecentViewedEconomicCalendarRes;
import com.sk.domain.shared.entity.master.tif.EconomicCalendar;
import com.sk.domain.shared.entity.master.tif.RefinitivEconomicCalendar;
import com.sk.domain.shared.entity.master.tlm.EconomicCalendarInquiryLog;
import com.sk.domain.shared.entity.master.tmd.EconomicIndicatorAnalysis;
import com.sk.domain.shared.entity.master.tmd.EconomicIndicatorAnalysisRelationship;
import com.sk.domain.shared.enums.CountryCurrency;
import com.sk.domain.shared.enums.EconomicCalendarDateRange;
import com.sk.domain.shared.enums.EconomicUnit;
import com.sk.domain.shared.enums.UnitPrefix;
import com.sk.domain.shared.enums.news.MarketCategoryCode;
import com.sk.domain.shared.repository.master.tif.EconomicCalendarRepository;
import com.sk.domain.shared.repository.master.tif.RefinitivEconomicCalendarRepository;
import com.sk.domain.shared.repository.master.tlm.EconomicCalendarInquiryLogRepository;
import com.sk.domain.shared.repository.master.tmd.EconomicIndicatorAnalysisRelationshipRepository;
import com.sk.domain.shared.repository.master.tmd.EconomicIndicatorAnalysisRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiEconomicCalendarService {

    private final EconomicCalendarRepository economicCalendarRepository;
    private final RefinitivEconomicCalendarRepository refinitivEconomicCalendarRepository;
    private final EconomicIndicatorAnalysisRepository economicIndicatorAnalysisRepository;
    private final EconomicIndicatorAnalysisRelationshipRepository economicIndicatorAnalysisRelationshipRepository;
    private final EconomicCalendarInquiryLogRepository economicCalendarInquiryLogRepository;

    /**
     * 경제 지표 달력 목록 조회 (통합 쿼리 사용)
     */
    public PaginationRes<EconomicCalendarListRes> getList(
        int page,
        int size,
        EconomicCalendarDateRange dateRange,
        String keyword,
        String categories,
        String importance
    ) {

        Pair<String, String> dateRangePair = dateRange.getDateRange();
        String startDate = dateRangePair.getLeft();
        String endDate = dateRangePair.getRight();

        List<String> categoryList = (categories != null && !categories.isBlank())
            ? Arrays.asList(categories.split(","))
            : List.of("");
        boolean filterByCategory =
            categories != null && !categoryList.isEmpty() && !categories.isBlank();

        List<Short> priorityList = parseImportance(importance);
        if (priorityList == null || priorityList.isEmpty()) {
            priorityList = List.of((short) -1);
        }
        boolean filterByImportance = importance != null && !importance.isBlank();

        int offset = (page - 1) * size;

        List<Object[]> results = economicCalendarRepository.findEconomicIndicatorsByPeriodWithStatus(
            startDate,
            endDate,
            keyword,
            categoryList,
            filterByCategory,
            priorityList,
            filterByImportance,
            size,
            offset
        );

        long totalCount = economicCalendarRepository.countEconomicIndicatorsByPeriodWithStatus(
            startDate,
            endDate,
            keyword,
            categoryList,
            filterByCategory,
            priorityList,
            filterByImportance
        );

        List<EconomicCalendarListRes> content = results.stream()
            .map(this::convertToListRes)
            .map(this::populateCountryInfo)
            .toList();

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<EconomicCalendarListRes> resultPage = new PageImpl<>(content, pageable, totalCount);

        return PaginationRes.toRes(resultPage);
    }

    /**
     * Object[] → EconomicCalendarListRes 변환
     */
    private EconomicCalendarListRes convertToListRes(Object[] row) {
        int idx = 0;
        return new EconomicCalendarListRes(
            ((Number) row[idx++]).longValue(),                    // economicCalendarId
            (String) row[idx++],                                   // code
            convertToLocalDateTime(row[idx++]),                    // date
            (String) row[idx++],                                   // economicIndicatorName
            (String) row[idx++],                                   // economicIndicatorPresentationDay
            (String) row[idx++],                                   // economicIndicatorPresentationTime
            (BigDecimal) row[idx++],                               // economicIndicatorRealValue
            (String) row[idx++],                                   // economicIndicatorPredictionDay
            (BigDecimal) row[idx++],                               // economicIndicatorPresentationPriorValue
            (BigDecimal) row[idx++],                               // economicIndicatorPredictionValue
            UnitPrefix.fromCode(
                (String) row[idx++]),                                   // unitPrefix
            EconomicUnit.fromCode((String) row[idx++]),                                   // unit
            (String) row[idx++],                                   // refinitivPresentationRegion
            row[idx] != null ? ((Number) row[idx]).shortValue() : null,  // refinitivPriority
            (String) row[++idx],                                   // type
            (String) row[++idx],                                   // status
            null,                                                  // countryNameCode (나중에 채움)
            null                                                   // countryCurrencyCode (나중에 채움)
        );
    }

    /**
     * Timestamp 또는 LocalDateTime을 LocalDateTime으로 변환
     */
    private LocalDateTime convertToLocalDateTime(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        if (dateObj instanceof Timestamp t) {
            return t.toLocalDateTime();
        }
        if (dateObj instanceof LocalDateTime ld) {
            return ld;
        }
        return null;
    }

    @Transactional
    public EconomicCalendarDetailRes getDetail(String code, int userNo) {
        log.debug("경제 지표 달력 상세 조회 요청 - code: {}, userNo: {}", code, userNo);

        EconomicCalendarDetailRes detail = economicCalendarRepository.findLatestEconomicCalendarByCode(
                code)
            .orElseThrow(() -> new IllegalArgumentException("해당 코드의 데이터를 찾을 수 없습니다: " + code));

        saveInquiryLog(userNo, code);

        return populateCountryInfoForDetail(detail);
    }

    private void saveInquiryLog(int userNo, String code) {
        try {
            EconomicCalendarInquiryLog log = EconomicCalendarInquiryLog.createLog(userNo, code);
            economicCalendarInquiryLogRepository.save(log);
        } catch (Exception e) {
            log.error("경제 지표 조회 로그 저장 중 오류 발생", e);
        }
    }


    /**
     * 경제 지표 달력 히스토리 조회
     */
    public PaginationRes<EconomicCalendarHistoryRes> getHistory(String code, int page, int size) {
        log.debug("경제 지표 달력 히스토리 조회 요청 - code: {}, page: {}, size: {}", code, page, size);

        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Direction.DESC, "date"));

        Page<EconomicCalendar> resultPage = economicCalendarRepository.findByCodeAndIsDeletedFalseOrderByDateDesc(
            code, pageable
        );

        Page<EconomicCalendarHistoryRes> historyPage = resultPage.map(this::convertToHistoryRes);

        return PaginationRes.toRes(historyPage);
    }

    /**
     * 주요 경제 지표 분석 조회 (기준날짜, 뉴스시장카테고리)
     */
    public KeyEconomicIndicatorsRes getKeyList(String criteriaDate, String category) {
        log.debug("주요 경제 지표 분석 조회 요청 - criteriaDate: {}, category: {}", criteriaDate, category);

        // 1. 경제 지표 분석 조회
        EconomicIndicatorAnalysis analysis = economicIndicatorAnalysisRepository
            .findByCriteriaDateAndCategory(criteriaDate, MarketCategoryCode.fromCode(category))
            .orElseThrow(() -> AppErrorException.of(
                Error.RSC_DATA_NOT_FOUND,
                "해당 기준일자(%s)와 카테고리(%s)의 경제 지표 분석 데이터를 찾을 수 없습니다"
                    .formatted(criteriaDate, category)
            ));

        // 2. 분석 내용 파싱 (줄바꿈으로 구분된 내용을 List로 변환)
        List<String> contentList = parseAnalysisContent(analysis.getAnalysisCntt());

        // 3. 관련 경제 지표 관계 조회
        List<EconomicIndicatorAnalysisRelationship> relationships =
            economicIndicatorAnalysisRelationshipRepository
                .findByEconomicIndicatorAnalysisWithEconomicCalendar(analysis);

        // 5. Refinitiv 경제 달력 정보 조회 (우선순위 정보 포함)
        Map<String, Short> codeToRefinitivPriorityMap = refinitivEconomicCalendarRepository
            .findByCodeIn(
                relationships.stream()
                    .map(r -> r.getEconomicCalendar().getCode())
                    .distinct()
                    .toList()
            )
            .stream()
            .collect(Collectors.toMap(
                RefinitivEconomicCalendar::getCode,
                rec -> rec.getPriority() != null ? rec.getPriority() : (short) 0
            ));

        // 6. 관련 경제 지표 DTO 변환
        List<RelatedEconomicIndicator> relatedIndicators = relationships.stream()
            .map(relationship -> convertToRelatedIndicator(
                relationship.getEconomicCalendar(),
                codeToRefinitivPriorityMap.getOrDefault(
                    relationship.getEconomicCalendar().getCode(),
                    (short) 0
                )
            ))
            .toList();

        // 7. 최종 응답 생성
        return new KeyEconomicIndicatorsRes(
            analysis.getEconomicIndicatorAnalysisId(),
            category,
            criteriaDate,
            contentList,
            relatedIndicators
        );
    }

    /**
     * importance 문자열을 priority 값 리스트로 변환 HIGH=3, MEDIUM=2, LOW=1
     */
    private List<Short> parseImportance(String importance) {
        if (importance == null || importance.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(importance.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .map(imp ->
                switch (imp) {
                    case "HIGH" -> (short) 3;
                    case "MEDIUM" -> (short) 2;
                    case "LOW" -> (short) 1;
                    default -> null;
                }
            )
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * 분석 내용 문자열을 List로 파싱 - 줄바꿈 문자(\n)로 구분 - 빈 줄 제거
     */
    private List<String> parseAnalysisContent(String analysisContent) {
        if (analysisContent == null || analysisContent.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(analysisContent.split("\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .toList();
    }

    /**
     * EconomicCalendar를 RelatedEconomicIndicator DTO로 변환
     */
    private RelatedEconomicIndicator convertToRelatedIndicator(
        EconomicCalendar economicCalendar,
        Short refinitivPriority
    ) {
        return new RelatedEconomicIndicator(
            economicCalendar.getEconomicCalendarId(),
            economicCalendar.getCode(),
            economicCalendar.getDate(),
            economicCalendar.getEconomicIndicatorName(),
            economicCalendar.getEconomicIndicatorPresentationDay(),
            economicCalendar.getEconomicIndicatorPresentationTime(),
            economicCalendar.getEconomicIndicatorRealValue(),
            economicCalendar.getEconomicIndicatorPredictionDay(),
            economicCalendar.getEconomicIndicatorPresentationPriorValue(),
            economicCalendar.getEconomicIndicatorPredictionValue(),
            UnitPrefix.fromCode(economicCalendar.getUnitPrefix()),
            EconomicUnit.fromCode(economicCalendar.getUnit()),
            refinitivPriority
        );
    }

    private EconomicCalendarListRes populateCountryInfo(EconomicCalendarListRes source) {
        if ((source.countryNameCode() != null && source.countryCurrencyCode() != null)
            || source.refinitivPresentationRegion() == null) {
            return source;
        }

        CountryCurrency countryCurrency = CountryCurrency.getByCountryName(
            source.refinitivPresentationRegion());
        if (countryCurrency == null) {
            countryCurrency = CountryCurrency.getByCountryCode(
                source.refinitivPresentationRegion());
        }

        if (countryCurrency == null) {
            return source;
        }

        return new EconomicCalendarListRes(
            source.economicCalendarId(),
            source.code(),
            source.date(),
            source.economicIndicatorName(),
            source.economicIndicatorPresentationDay(),
            source.economicIndicatorPresentationTime(),
            source.economicIndicatorRealValue(),
            source.economicIndicatorPredictionDay(),
            source.economicIndicatorPresentationPriorValue(),
            source.economicIndicatorPredictionValue(),
            source.unitPrefix(),
            source.unit(),
            source.refinitivPresentationRegion(),
            source.refinitivPriority(),
            source.refinitivType(),
            source.status(),
            countryCurrency.getCountryCode(),
            countryCurrency.getCurrencyCode()
        );
    }

    private EconomicCalendarDetailRes populateCountryInfoForDetail(
        EconomicCalendarDetailRes source) {
        if ((source.countryNameCode() != null && source.countryCurrencyCode() != null)
            || source.refinitivPresentationRegion() == null) {
            return source;
        }

        CountryCurrency countryCurrency = CountryCurrency.getByCountryName(
            source.refinitivPresentationRegion());
        if (countryCurrency == null) {
            countryCurrency = CountryCurrency.getByCountryCode(
                source.refinitivPresentationRegion());
        }

        if (countryCurrency == null) {
            return source;
        }

        return new EconomicCalendarDetailRes(
            source.economicCalendarId(),
            source.code(),
            source.date(),
            source.economicIndicatorName(),
            source.economicIndicatorPresentationDay(),
            source.economicIndicatorPresentationTime(),
            source.economicIndicatorRealValue(),
            source.economicIndicatorPredictionDay(),
            source.economicIndicatorPresentationPriorValue(),
            source.economicIndicatorPredictionValue(),
            source.unitPrefix() == null ? null
                : UnitPrefix.fromCode(source.unitPrefix()).getDisplayValue(),
            source.unit() == null ? null : EconomicUnit.fromCode(source.unit()).getAbbreviation(),
            source.nextEconomicIndicatorPresentationDay(),
            source.refinitivName(),
            source.refinitivPresentationRegion(),
            source.refinitivExplanation(),
            source.refinitivSourceOrganization(),
            source.refinitivType(),
            source.refinitivUnit(),
            source.refinitivPresentationCycle(),
            source.refinitivIsUsed(),
            source.refinitivPriority(),
            source.refinitivPresentationDatetime(),
            countryCurrency.getCountryCode(),
            countryCurrency.getCurrencyCode()
        );
    }

    /**
     * EconomicCalendar 엔티티를 EconomicCalendarHistoryRes DTO로 변환
     */
    private EconomicCalendarHistoryRes convertToHistoryRes(EconomicCalendar entity) {
        return new EconomicCalendarHistoryRes(
            entity.getEconomicCalendarId(),
            entity.getCode(),
            entity.getDate(),
            entity.getEconomicIndicatorName(),
            entity.getEconomicIndicatorPresentationDay(),
            entity.getEconomicIndicatorPresentationTime(),
            entity.getEconomicIndicatorRealValue(),
            entity.getEconomicIndicatorPredictionDay(),
            entity.getEconomicIndicatorPresentationPriorValue(),
            entity.getEconomicIndicatorPredictionValue(),
            UnitPrefix.fromCode(entity.getUnitPrefix()),
            EconomicUnit.fromCode(entity.getUnit()),
            entity.getNextEconomicIndicatorPresentationDay()
        );
    }

    /**
     * 위젯용 경제 지표 달력 목록 조회 (TODAY 고정, 페이징 없음)
     */
    public EconomicCalendarWidgetRes getWidgetList(
        String criteriaDate,
        String categories,
        String importance
    ) {
        log.debug("위젯용 경제 지표 달력 조회 - categories: {}, importance: {}",
            categories, importance);

        String today;

        if (criteriaDate == null || criteriaDate.isBlank()) {
            today = DateUtil.toYmdHyphen(LocalDate.now());
        } else {
            LocalDate cDate = DateUtil.fromYmd(criteriaDate);
            today = DateUtil.toYmdHyphen(cDate);
        }

        List<String> categoryList = (categories != null && !categories.isBlank())
            ? Arrays.asList(categories.split(","))
            : List.of("");

        boolean filterByCategory =
            categories != null && !categoryList.isEmpty() && !categories.isBlank();

        List<Short> priorityList = parseImportance(importance);
        if (priorityList == null || priorityList.isEmpty()) {
            priorityList = List.of((short) -1);
        }
        boolean filterByImportance = importance != null && !importance.isBlank();

        List<Object[]> results = economicCalendarRepository.findEconomicIndicatorsByPeriodWithStatus(
            today,
            today,
            "",
            categoryList,
            filterByCategory,
            priorityList,
            filterByImportance,
            1000,  // limit
            0      // offset
        );

        String updateTime = generateUpdateTime(results);

        return new EconomicCalendarWidgetRes(
            updateTime,
            results.stream()
                .map(this::convertToWidgetRes)
                .toList()
        );
    }

    /**
     * EconomicCalendarListRes를 위젯용 DTO로 변환
     */
    private EconomicCalendarWidget convertToWidgetRes(Object[] row) {
        int idx = 0;

        Long economicCalendarId = ((Number) row[idx++]).longValue();
        String code = (String) row[idx++];
        idx++;  // date 스킵
        String economicIndicatorName = (String) row[idx++];
        String economicIndicatorPresentationDay = (String) row[idx++];
        String economicIndicatorPresentationTime = (String) row[idx++];
        BigDecimal economicIndicatorRealValue = (BigDecimal) row[idx++];
        idx++;  // economicIndicatorPredictionDay 스킵
        BigDecimal economicIndicatorPresentationPriorValue = (BigDecimal) row[idx++];
        BigDecimal economicIndicatorPredictionValue = (BigDecimal) row[idx++];
        String unitPrefix = (String) row[idx++];
        String unit = (String) row[idx++];
        idx++;  // refinitivPresentationRegion 스킵
        Short refinitivPriority = row[idx] != null ? ((Number) row[idx]).shortValue() : null;
        idx++;
        String refinitivType = (String) row[idx++];
        String status = (String) row[idx];

        return new EconomicCalendarWidget(
            economicCalendarId,
            code,
            economicIndicatorName,
            economicIndicatorPresentationDay,
            economicIndicatorPresentationTime,
            economicIndicatorRealValue,
            economicIndicatorPresentationPriorValue,
            economicIndicatorPredictionValue,
            UnitPrefix.fromCode(unitPrefix),
            EconomicUnit.fromCode(unit),
            refinitivPriority,
            refinitivType,
            status
        );
    }

    /**
     * results에서 마지막 ANNOUNCED 상태 항목의 발표 일시를 기반으로 updateTime 생성 형식: "Sep 15 09:45"
     */
    private String generateUpdateTime(List<Object[]> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        // results를 역순으로 탐색하여 마지막 ANNOUNCED 항목 찾기
        for (int i = results.size() - 1; i >= 0; i--) {
            Object[] row = results.get(i);

            // status는 row 배열의 마지막 인덱스 (15번)
            String status = (String) row[15];

            if ("ANNOUNCED".equals(status)) {
                // economicIndicatorPresentationDay는 인덱스 4
                // economicIndicatorPresentationTime은 인덱스 5
                String presentationDay = (String) row[4];  // "20250915" 형식
                String presentationTime = (String) row[5]; // "09:45:00" 형식

                return formatUpdateTime(presentationDay, presentationTime);
            }
        }

        // ANNOUNCED 항목이 없을 경우 빈 문자열 반환
        return "";
    }

    /**
     * 발표 일자와 시간을 "Sep 15 09:45" 형식으로 변환
     *
     * @param presentationDay  "2025-09-16" 형식 (하이픈 포함)
     * @param presentationTime "14:00:00" 형식
     * @return "Sep 16 14:00" 형식
     */
    private String formatUpdateTime(String presentationDay, String presentationTime) {
        if (presentationDay == null || presentationDay.isEmpty()) {
            return "";
        }

        try {
            // 날짜 파싱: "2025-09-16" -> LocalDate
            LocalDate date = LocalDate.parse(presentationDay);

            // 시간 추출: "14:00:00" -> "14:00"
            String time = (presentationTime != null && presentationTime.length() >= 5)
                ? presentationTime.substring(0, 5)
                : "";

            // DateTimeFormatter로 "Sep 16" 형식 생성
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
            String formattedDate = date.format(formatter);

            // 결과 조합: "Sep 16 14:00"
            return String.format("%s %s", formattedDate, time);

        } catch (Exception e) {
            log.error("updateTime 포맷 변환 중 오류 발생 - presentationDay: {}, presentationTime: {}",
                presentationDay, presentationTime, e);
            return "";
        }
    }

    /**
     * 사용자의 최근 조회 경제 달력 목록 5개 조회 (Join 쿼리를 통한 최적화)
     */
    @Transactional(readOnly = true)
    public List<RecentViewedEconomicCalendarRes> getRecentViewedList(int userNo) {
        log.debug("최근 조회 경제 달력 5개 요청(최적화) - userNo: {}", userNo);

        return economicCalendarInquiryLogRepository.findTop5RecentViewedWithSource(
            userNo,
            PageRequest.of(0, 5)
        );
    }

    /**
     * 최근 조회 내역 전체 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteAllRecentViewed(int userNo) {
        economicCalendarInquiryLogRepository.softDeleteAllByUserNo(userNo);
    }
}
