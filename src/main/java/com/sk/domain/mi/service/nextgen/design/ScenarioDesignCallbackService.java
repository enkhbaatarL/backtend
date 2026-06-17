package com.sk.domain.mi.service.nextgen.design;

import com.sk.domain.mi.dto.request.nextgen.design.ScenarioGenerationCallbackReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsCallbackReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationCallbackReq;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent.Stage;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent.Type;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioValidationCache;
import com.sk.domain.mi.service.nextgen.sse.ScenarioDesignEventPublisher;
import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 시나리오 설계 Agent 콜백 처리.
 * <p>
 * 콜백은 신호다 — 영속화 주체는 Agent이며(검토는 DB 미저장), B.E는 DB에 쓰지 않는다.
 * 처리: (검토는 결과를 Redis 휘발 캐시에 저장) + 설계 세션 SSE 푸시 발행. FE는 푸시 수신 후 세션 상태를 재조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioDesignCallbackService {

    private final ScenarioDesignValidationStore validationStore;
    private final ScenarioDesignEventPublisher eventPublisher;

    /** 생성 완료: stage scenario_created 푸시. */
    public void handleGeneration(Long scenarioGroupId, ScenarioGenerationCallbackReq request) {
        if (jobFailed(scenarioGroupId, Type.GENERATION, request.result(), request.reason())) {
            return;
        }
        eventPublisher.publish(ScenarioDesignSseEvent.of(scenarioGroupId, Type.GENERATION, Stage.SCENARIO_CREATED));
    }

    /**
     * 검토 완료: 결과(table + isValid/reason)를 Redis에 저장한 뒤 푸시한다.
     * <p>
     * 저장 -> 푸시 순서를 지킨다 — FE가 푸시 수신 직후 재조회할 때 캐시가 이미 보장되도록(레이스 방지).
     * isValid=true면 scenario_validated, false면 scenario_created 유지.
     */
    public void handleValidation(Long scenarioGroupId, ScenarioValidationCallbackReq request) {
        if (jobFailed(scenarioGroupId, Type.VALIDATION, request.result(), request.reason())) {
            return;
        }
        validationStore.save(scenarioGroupId, ScenarioValidationCache.of(request.isValid(), request.reason(), request.table()));

        Stage stage = Boolean.TRUE.equals(request.isValid()) ? Stage.SCENARIO_VALIDATED : Stage.SCENARIO_CREATED;
        eventPublisher.publish(ScenarioDesignSseEvent.of(scenarioGroupId, Type.VALIDATION, stage));
    }

    /** Signpost 생성 완료: stage signpost_created 푸시. */
    public void handleSignposts(Long scenarioGroupId, ScenarioSignpostsCallbackReq request) {
        if (jobFailed(scenarioGroupId, Type.SIGNPOSTS, request.result(), request.reason())) {
            return;
        }
        eventPublisher.publish(ScenarioDesignSseEvent.of(scenarioGroupId, Type.SIGNPOSTS, Stage.SIGNPOST_CREATED));
    }

    /**
     * Agent 작업 실패(result=failure) 여부. 실패면 푸시를 스킵하고 로깅만 한다.
     * <p>
     * (실패 전용 신호 체계는 아직 미정 — 우선 로깅. 검토 정합성 실패(isValid=false)는 작업 성공의 정상 경로로 별도 처리)
     */
    private boolean jobFailed(Long scenarioGroupId, Type type, AgentCallbackResult result, String reason) {
        if (result != null && result.isSuccess()) return false;
        log.warn("[Design Callback] agent job failed: type={}, groupId={}, reason={}", type.getCode(), scenarioGroupId, reason);
        return true;
    }

}
