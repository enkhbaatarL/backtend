package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.priceinterp.PriceInterpretationEditReq;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationGenerateRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSectionRes;

/**
 * 시나리오별 가격전망 해석 서비스.
 */
public interface PriceInterpretationService {

    /** [조회] 가격전망 해석 조회 */
    PriceInterpretationSectionRes getInterpretations(Long scenarioGroupId);

    /** [생성] 대상 시나리오를 generating으로 생성하고 Agent에 해석 산출을 요청 */
    PriceInterpretationGenerateRes generateInterpretation(Long scenarioGroupId);

    /** [수정] 사용자 수정본 반영 */
    PriceInterpretationRes editInterpretation(Long scenarioGroupId, Long scenarioId, PriceInterpretationEditReq request);

    /** [초기화] Agent 생성 원본으로 복원 */
    PriceInterpretationRes resetInterpretation(Long scenarioGroupId, Long scenarioId);

}
