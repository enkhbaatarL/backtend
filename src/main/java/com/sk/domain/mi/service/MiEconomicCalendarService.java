package com.sk.domain.mi.service;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes;
import com.sk.domain.shared.enums.CountryCurrency;
import com.sk.domain.shared.enums.EconomicCalendarDateRange;
import com.sk.domain.shared.repository.master.tif.EconomicCalendarRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiEconomicCalendarService {

    private final EconomicCalendarRepository economicCalendarRepository;

    /**
     * 경제 지표 달력 목록 조회
     */
    public PaginationRes<EconomicCalendarListRes> getList(
        int page,
        int size,
        EconomicCalendarDateRange dateRange,
        String keyword,
        String categories,
        String importance
    ) {

        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Direction.ASC, "economicIndicatorPresentationTime", "economicCalendarId"));

        Pair<String, String> dateRangePair = dateRange.getDateRange();
        String startDate = dateRangePair.getLeft();
        String endDate = dateRangePair.getRight();

        List<String> categoryList = categories != null ? Arrays.asList(categories.split(",")) : null;
        boolean filterByCategory = categories != null && !categoryList.isEmpty();

        List<Short> priorityList = parseImportance(importance);
        boolean filterByImportance = importance != null && priorityList != null && !priorityList.isEmpty();

        Page<EconomicCalendarListRes> resultPage;

        // TOMORROW와 NEXTWEEK는 nextEconomicIndicatorPresentationDay 컬럼 사용
        boolean useNextDay = dateRange == EconomicCalendarDateRange.TOMORROW
            || dateRange == EconomicCalendarDateRange.NEXTWEEK;

        if (useNextDay) {
            // nextEconomicIndicatorPresentationDay 컬럼 사용
            if (startDate.equals(endDate)) {
                resultPage = economicCalendarRepository.findEconomicCalendarsWithRefinitivByNextDay(
                    startDate,
                    keyword,
                    filterByCategory ? categoryList : null,
                    filterByCategory,
                    filterByImportance ? priorityList : null,
                    filterByImportance,
                    pageable
                ).map(this::populateCountryInfo);
            } else {
                resultPage = economicCalendarRepository.findEconomicCalendarsWithRefinitivByNextDayRange(
                    startDate,
                    endDate,
                    keyword,
                    filterByCategory ? categoryList : null,
                    filterByCategory,
                    filterByImportance ? priorityList : null,
                    filterByImportance,
                    pageable
                ).map(this::populateCountryInfo);
            }
        } else {
            // economicIndicatorPresentationDay 컬럼 사용
            if (startDate.equals(endDate)) {
                resultPage = economicCalendarRepository.findEconomicCalendarsWithRefinitiv(
                    startDate,
                    keyword,
                    filterByCategory ? categoryList : null,
                    filterByCategory,
                    filterByImportance ? priorityList : null,
                    filterByImportance,
                    pageable
                ).map(this::populateCountryInfo);
            } else {
                resultPage = economicCalendarRepository.findEconomicCalendarsWithRefinitivByDateRange(
                    startDate,
                    endDate,
                    keyword,
                    filterByCategory ? categoryList : null,
                    filterByCategory,
                    filterByImportance ? priorityList : null,
                    filterByImportance,
                    pageable
                ).map(this::populateCountryInfo);
            }
        }

        return PaginationRes.toRes(resultPage);
    }

    /**
     * importance 문자열을 priority 값 리스트로 변환
     * HIGH=3, MEDIUM=2, LOW=1
     */
    private List<Short> parseImportance(String importance) {
        if (importance == null || importance.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(importance.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .map(imp -> {
                return switch (imp) {
                    case "HIGH" -> (short) 3;
                    case "MEDIUM" -> (short) 2;
                    case "LOW" -> (short) 1;
                    default -> null;
                };
            })
            .filter(Objects::nonNull)
            .toList();
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
}
