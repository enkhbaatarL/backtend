package com.sk.domain.mi.service.nextgen.mock;

import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.priceinterp.PriceInterpretationEditReq;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationGenerateRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSectionRes;
import com.sk.domain.mi.service.nextgen.PriceInterpretationService;
import com.sk.domain.mi.service.nextgen.PriceInterpretationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 가격전망 해석 서비스 위임자.
 */
@Service
@Primary
@RequiredArgsConstructor
public class PriceInterpretationServiceDelegator implements PriceInterpretationService {

    private final PriceInterpretationServiceImpl realService;
    private final PriceInterpretationMockService mockService;
    private final NextGenMockProperties mockProps;

    private NextGenMockProperties.PriceInterpretations.Api api() {
        return mockProps.getPriceInterpretations().getApi();
    }

    @Override
    public PriceInterpretationSectionRes getInterpretations(Long scenarioGroupId) {
        return realService.getInterpretations(scenarioGroupId);
    }

    @Override
    public PriceInterpretationGenerateRes generateInterpretation(Long scenarioGroupId) {
        return api().isGenerate()
            ? mockService.generateInterpretation(scenarioGroupId)
            : realService.generateInterpretation(scenarioGroupId);
    }

    @Override
    public PriceInterpretationRes editInterpretation(Long scenarioGroupId, Long scenarioId, PriceInterpretationEditReq request) {
        return realService.editInterpretation(scenarioGroupId, scenarioId, request);
    }

    @Override
    public PriceInterpretationRes resetInterpretation(Long scenarioGroupId, Long scenarioId) {
        return realService.resetInterpretation(scenarioGroupId, scenarioId);
    }

}
