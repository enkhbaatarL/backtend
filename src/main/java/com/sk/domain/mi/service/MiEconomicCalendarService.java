package com.sk.domain.mi.service;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes;
import com.sk.domain.shared.enums.CountryCurrency;
import com.sk.domain.shared.repository.master.tif.EconomicCalendarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 경제 지표 달력 목록 조회 (페이징)
     */
    public PaginationRes<EconomicCalendarListRes> getList(
            int page,
            int size,
        String date
        ) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "economicIndicatorPresentationTime", "economicCalendarId"));

        Page<EconomicCalendarListRes> resultPage = economicCalendarRepository.findEconomicCalendarsWithRefinitiv(
                date,
                pageable
        ).map(this::populateCountryInfo);

        return PaginationRes.toRes(resultPage);
    }

    private EconomicCalendarListRes populateCountryInfo(EconomicCalendarListRes source) {
        if ((source.countryNameCode() != null && source.countryCurrencyCode() != null)
                || source.refinitivPresentationRegion() == null) {
            return source;
        }

        CountryCurrency countryCurrency = CountryCurrency.getByCountryName(source.refinitivPresentationRegion());
        if (countryCurrency == null) {
            countryCurrency = CountryCurrency.getByCountryCode(source.refinitivPresentationRegion());
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
