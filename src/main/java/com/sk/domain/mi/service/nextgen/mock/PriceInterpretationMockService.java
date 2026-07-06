package com.sk.domain.mi.service.nextgen.mock;

import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.priceinterp.PriceInterpretationGenerationCallbackReq;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationGenerateRes;
import com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationCallbackService;
import com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationStore;
import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 가격전망 해석 Mock 서비스
 * DB 영속화·콜백 처리·SSE 발행은 실 로직(PriceInterpretationStore/CallbackService)을 그대로 쓰고, Agent 호출만 대체한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceInterpretationMockService {

    private final PriceInterpretationStore store;
    private final PriceInterpretationCallbackService callbackService;
    private final NextGenMockProperties mockProperties;

    /** Agent 콜백 도착을 흉내내는 지연 스케줄러 (데몬). */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "priceinterp-mock-callback");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    void destroy() {
        scheduler.shutdownNow();
    }

    public PriceInterpretationGenerateRes generateInterpretation(Long scenarioGroupId) {
        List<Long> targets = store.beginGeneration(scenarioGroupId);
        simulateAgentCallback(scenarioGroupId, targets);
        return PriceInterpretationGenerateRes.of(scenarioGroupId, targets);
    }

    /** Agent 비동기 콜백을 지연 후 시뮬레이션한다 (내부 서비스 호출 — 실제 콜백 API를 쏘지 않음). */
    private void simulateAgentCallback(Long scenarioGroupId, List<Long> targets) {
        long delaySeconds = mockProperties.getPriceInterpretations().getCallbackDelaySeconds();

        scheduler.schedule(() -> {
            try {
                PriceInterpretationGenerationCallbackReq request =
                    new PriceInterpretationGenerationCallbackReq(AgentCallbackResult.SUCCESS, null, mockItems(targets));
                callbackService.handleGeneration(scenarioGroupId, request);
                log.info("[Mock] Simulated price-interpretation callback: groupId={}, targets={}", scenarioGroupId, targets);
            } catch (Exception e) {
                log.error("[Mock] Simulated price-interpretation callback failed: groupId={}", scenarioGroupId, e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private List<PriceInterpretationGenerationCallbackReq.Item> mockItems(List<Long> scenarioIds) {
        return scenarioIds.stream()
            .map(scenarioId -> new PriceInterpretationGenerationCallbackReq.Item(scenarioId, mockContent(scenarioId)))
            .toList();
    }

    private static final List<String> MOCK_CONTENTS = List.of(
        """
        [mock] 시나리오 %d 가격전망 해석: 단기 구간에서는 공급 불확실성에 따른 리스크 프리미엄이 일부 반영되지만, 생산 및 선적 일정이 정상화되면서 가격은 점진적으로 MI Base 전망선에 수렴하는 흐름으로 해석됩니다. 참고로 이 문장은 mock 응답입니다.""",
        """
        [mock] 시나리오 %d 가격전망 해석: 해당 시나리오에서는 주요 수출 터미널의 공급 차질이 장기화되며 초반 강세가 두드러집니다. 동절기 재고 확보 수요와 맞물려 스팟 프리미엄이 확대되고, 공급 정상화가 확인되는 시점부터 완만한 하향 안정세로 전환될 것으로 해석됩니다. 참고로 이 문장은 mock 응답입니다.""",
        """
        [mock] 시나리오 %d 가격전망 해석: 수요 둔화 가정이 우세한 시나리오로, 아시아 주요 수입국의 재고 여유와 온화한 기온 전망이 겹치며 가격은 MI Base 대비 하방 압력이 지속됩니다. 다만 신규 액화 설비의 램프업 지연이 하단을 지지해 급락보다는 박스권 약세 흐름으로 해석됩니다. 참고로 이 문장은 mock 응답입니다.""",
        """
        [mock] 시나리오 %d 가격전망 해석: 수급 양방향 변수가 혼재해 변동성 확대가 핵심인 시나리오입니다. 지정학 리스크에 따른 단기 스파이크 가능성과 신규 공급 유입에 따른 조정 가능성이 교차하며, 분기별로 MI Base 전망선을 상회·하회를 반복하는 높은 변동성 구간이 이어질 것으로 해석됩니다. 참고로 이 문장은 mock 응답입니다."""
    );

    private String mockContent(Long scenarioId) {
        return MOCK_CONTENTS.get((int) (scenarioId % MOCK_CONTENTS.size())).formatted(scenarioId);
    }

}
