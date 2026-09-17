package com.sk.domain.mi.service.nextgen.sse;

import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalSseEvent;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSseEvent;
import com.sk.domain.mi.service.nextgen.sse.registry.GroupModelParamProposalSseRegistry;
import com.sk.domain.mi.service.nextgen.sse.registry.ModelParamProposalSseRegistry;
import com.sk.domain.mi.service.nextgen.sse.registry.PriceInterpretationSseRegistry;
import com.sk.domain.mi.service.nextgen.sse.registry.ScenarioDesignSseRegistry;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * SSE 이벤트 구독자
 *
 * Redis 채널에서 이벤트를 수신해 채널 prefix로 도메인을 판별한 뒤, 노드가 보유한 해당 세션 SSE 연결로 전달한다.
 * 연결이 없는 노드에서는 registry.emit이 no-op이므로 안전하게 무시된다.
 *
 * @see SseEventPublisher 발행자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventSubscriber implements MessageListener {

    private final ScenarioDesignSseRegistry designSseRegistry;
    private final PriceInterpretationSseRegistry priceInterpSseRegistry;
    private final ModelParamProposalSseRegistry modelParamProposalSseRegistry;
    private final GroupModelParamProposalSseRegistry groupModelParamProposalSseRegistry;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        if (channel.startsWith(SseEventPublisher.DESIGN_CHANNEL_PREFIX)) {
            try {
                ScenarioDesignSseEvent event = JsonUtil.fromJson(body, ScenarioDesignSseEvent.class);
                designSseRegistry.emit(event.scenarioGroupId(), event);
            } catch (Exception e) {
                log.warn("[Design SSE] subscribe failed (deserialize): {}", e.getMessage());
            }
            return;
        }

        if (channel.startsWith(SseEventPublisher.PRICE_INTERP_CHANNEL_PREFIX)) {
            try {
                PriceInterpretationSseEvent event = JsonUtil.fromJson(body, PriceInterpretationSseEvent.class);
                priceInterpSseRegistry.emit(event.scenarioGroupId(), event);
            } catch (Exception e) {
                log.warn("[PriceInterp SSE] subscribe failed (deserialize): {}", e.getMessage());
            }
            return;
        }

        if (channel.startsWith(SseEventPublisher.MODEL_PARAM_PROPOSAL_CHANNEL_PREFIX)) {
            try {
                ModelParamProposalSseEvent event = JsonUtil.fromJson(body, ModelParamProposalSseEvent.class);
                modelParamProposalSseRegistry.emit(event.scenarioId(), event);
                // 그룹 스트림 팬아웃 — scenarioGroupId가 없는 구버전 발행분은 시나리오 스트림에만 전달된다
                if (event.scenarioGroupId() != null) {
                    groupModelParamProposalSseRegistry.emit(event.scenarioGroupId(), event);
                }
            } catch (Exception e) {
                log.warn("[ModelParamProposal SSE] subscribe failed (deserialize): {}", e.getMessage());
            }
            return;
        }

        // 새 SSE 도메인 추가 시 분기 누락이 침묵 유실되지 않도록 경보
        log.warn("[SSE] unknown channel, message dropped: {}", channel);
    }

}
