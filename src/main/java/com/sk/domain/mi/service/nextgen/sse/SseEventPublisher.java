package com.sk.domain.mi.service.nextgen.sse;

import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalSseEvent;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * SSE 이벤트 발행자
 *
 * 콜백 도착 시 이 노드에서 직접 emit 하지 않고 도메인별 Redis 채널로 발행한다.
 * 발행 노드 포함 모든 노드의 구독자가 수신하므로, 해당 세션 연결을 보유한 노드에서만 실제 SSE 푸시가 일어난다.
 *
 * @see SseEventSubscriber 구독자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventPublisher {

    public static final String DESIGN_CHANNEL_PREFIX = "scenario-design:";
    public static final String DESIGN_CHANNEL_PATTERN = DESIGN_CHANNEL_PREFIX + "*";

    public static final String PRICE_INTERP_CHANNEL_PREFIX = "scenario-price-interp:";
    public static final String PRICE_INTERP_CHANNEL_PATTERN = PRICE_INTERP_CHANNEL_PREFIX + "*";

    public static final String MODEL_PARAM_PROPOSAL_CHANNEL_PREFIX = "model-param-proposal:";
    public static final String MODEL_PARAM_PROPOSAL_CHANNEL_PATTERN = MODEL_PARAM_PROPOSAL_CHANNEL_PREFIX + "*";

    private final StringRedisTemplate stringRedisTemplate;

    public static String designChannel(Long scenarioGroupId) {
        return DESIGN_CHANNEL_PREFIX + scenarioGroupId;
    }

    public static String priceInterpChannel(Long scenarioGroupId) {
        return PRICE_INTERP_CHANNEL_PREFIX + scenarioGroupId;
    }

    public static String modelParamProposalChannel(Long scenarioId) {
        return MODEL_PARAM_PROPOSAL_CHANNEL_PREFIX + scenarioId;
    }

    /**
     * 설계 세션 이벤트를 Redis 채널로 발행한다.
     */
    public void publish(ScenarioDesignSseEvent event) {
        try {
            String body = JsonUtil.toJson(event);
            Long receivedBy = stringRedisTemplate.convertAndSend(designChannel(event.scenarioGroupId()), body);
            log.debug("[Design SSE] published: groupId={}, type={}, stage={}, receivedBy={}", event.scenarioGroupId(), event.type(), event.stage(), receivedBy);
            alertIfNoSubscriber("[Design SSE]", receivedBy);
        } catch (Exception e) {
            log.warn("[Design SSE] publish failed (serialize): groupId={}, {}", event.scenarioGroupId(), e.getMessage());
        }
    }

    /**
     * 가격전망 해석 이벤트를 Redis 채널로 발행한다.
     */
    public void publish(PriceInterpretationSseEvent event) {
        try {
            String body = JsonUtil.toJson(event);
            Long receivedBy = stringRedisTemplate.convertAndSend(priceInterpChannel(event.scenarioGroupId()), body);
            log.debug("[PriceInterp SSE] published: groupId={}, type={}, result={}, receivedBy={}", event.scenarioGroupId(), event.type(), event.result(), receivedBy);
            alertIfNoSubscriber("[PriceInterp SSE]", receivedBy);
        } catch (Exception e) {
            log.warn("[PriceInterp SSE] publish failed (serialize): groupId={}, {}", event.scenarioGroupId(), e.getMessage());
        }
    }

    /**
     * 수급모델 파라미터 제안 완료/실패 신호를 Redis 채널로 발행한다.
     */
    public void publish(ModelParamProposalSseEvent event) {
        try {
            String body = JsonUtil.toJson(event);
            Long receivedBy = stringRedisTemplate.convertAndSend(modelParamProposalChannel(event.scenarioId()), body);
            log.debug("[ModelParamProposal SSE] published: scenarioId={}, modelParamProposalId={}, status={}, receivedBy={}", event.scenarioId(), event.modelParamProposalId(), event.status(), receivedBy);
            alertIfNoSubscriber("[ModelParamProposal SSE]", receivedBy);
        } catch (Exception e) {
            log.warn("[ModelParamProposal SSE] publish failed (serialize): scenarioId={}, {}", event.scenarioId(), e.getMessage());
        }
    }

    /**
     * 수신자 없음 경보.
     * convertAndSend는 메시지를 받은 구독 클라이언트 수를 돌려준다. 0이면 이 노드를 포함해 어느 노드도
     * 해당 채널을 구독하고 있지 않다는 뜻이고, 발행이 성공해도 이벤트는 그대로 유실된다.
     */
    private void alertIfNoSubscriber(String tag, Long receivedBy) {
        if (receivedBy != null && receivedBy > 0) {
            return;
        }
        log.error("{} no subscriber - event is lost. The Redis subscription connection of this node may be dead. receivedBy={}", tag, receivedBy);
    }

}
