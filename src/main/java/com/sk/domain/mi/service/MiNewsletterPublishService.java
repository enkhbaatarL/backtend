package com.sk.domain.mi.service;

import com.sk.common.properties.ChartCaptureProperties;
import com.sk.domain.shared.client.ChartRendererClient;
import com.sk.domain.shared.service.ChartCaptureJwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 뉴스레터 발행 처리 서비스.
 *
 * <p>Airflow DAG이 발행 완료 후 trigger-mail API를 호출하면,
 * {@link #captureAndSendAsync}가 비동기로 실행되어
 * Playwright 차트 캡처 → 구독자 개별 메일 발송 까지 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiNewsletterPublishService {

    private final ChartCaptureJwtService chartCaptureJwtService;
    private final ChartCaptureProperties chartCaptureProperties;
    private final ChartRendererClient chartRendererClient;
    private final MiNewsletterMailService miNewsletterMailService;

    /**
     * 차트 캡처 + 메일 발송 비동기 실행.
     *
     * <ol>
     *   <li>단기 JWT(5분 TTL) 발급 (newsletterId + issueNo)</li>
     *   <li>프론트엔드 렌더 URL 구성 후 Playwright로 {@code window.__mailContents} 추출</li>
     *   <li>구독자에게 개별 발송 (각자 본인만 To 필드에 노출)</li>
     * </ol>
     *
     * <p>예외 발생 시 로그만 기록하고 종료한다 ({@link com.sk.common.config.AsyncConfig} uncaught handler 참조).
     * TODO: 발송 실패 이력을 DB에 저장하는 로직 추가 필요.
     *
     * @param newsletterId 뉴스레터 마스터 ID
     * @param issueNo      회차 번호
     * @param subject      메일 제목
     */
    @Async
    public void captureAndSendAsync(Integer newsletterId, Integer issueNo, String subject) {
        log.info("Newsletter capture+send started. newsletterId={}, issueNo={}", newsletterId, issueNo);

        try {
            String jwt = chartCaptureJwtService.issueToken(newsletterId, issueNo);
            String renderUrl = chartCaptureProperties.getFrontendBaseUrl()
                    + "/newsletter-chart-render?token=" + jwt;

            log.info("Playwright capture started. renderUrl={}", renderUrl);
            ChartRendererClient.MailContents mailContents = chartRendererClient.capture(renderUrl);

            miNewsletterMailService.sendOriginalIssueMailFromCapture(newsletterId, issueNo, subject, mailContents);

            log.info("Newsletter capture+send completed. newsletterId={}, issueNo={}", newsletterId, issueNo);

        } catch (Exception e) {
            // TODO: 발송 실패 이력 저장
            log.error("Newsletter capture+send failed. newsletterId={}, issueNo={}", newsletterId, issueNo, e);
        }
    }
}
