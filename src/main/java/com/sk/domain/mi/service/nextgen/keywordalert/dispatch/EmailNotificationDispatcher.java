package com.sk.domain.mi.service.nextgen.keywordalert.dispatch;

import com.sk.domain.shared.enums.notification.NotificationChannel;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.repository.master.tmm.projection.KeywordAlertMailEventRow;
import com.sk.domain.shared.service.MailService;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * 키워드 알림 EMAIL 디스패처
 * - 수신: 사용자 로그인 ID
 * - 본문: templates/event-keyword-alert.html (Thymeleaf), 로고 cid:logo 인라인
 * - 이력: tmd_mi_evnt_keywdsendl
 */
@Component
public class EmailNotificationDispatcher extends AbstractNotificationDispatcher {

    static final String TEMPLATE = "event-keyword-alert";

    /** 메일 [이벤트 상세보기] 링크의 FE 라우트 (server.domain 뒤에 붙음, %d = 이벤트 ID). 예: https://mi.parkjh.co.kr/mi/next-gen-mi/events/1704 */
    static final String EVENT_DETAIL_PATH = "/mi/next-gen-mi/events/%d";

    private static final DateTimeFormatter OCCURRED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MailService mailService;
    private final SpringTemplateEngine templateEngine;
    private final MiEventRepository miEventRepository;
    private final String serverDomain;

    public EmailNotificationDispatcher(
            NotificationDeliveryStateService stateService,
            NotificationDispatchProperties properties,
            MailService mailService,
            SpringTemplateEngine templateEngine,
            MiEventRepository miEventRepository,
            @Value("${server.domain}") String serverDomain
    ) {
        super(stateService, properties);
        this.mailService = mailService;
        this.templateEngine = templateEngine;
        this.miEventRepository = miEventRepository;
        this.serverDomain = serverDomain;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    protected void send(NotificationDispatchTarget target) {
        if (StringUtils.isBlank(target.userLoginId())) {
            throw new IllegalStateException("수신자 로그인 ID(이메일)가 없습니다. userNo=%d".formatted(target.userNo()));
        }
        String body = templateEngine.process(TEMPLATE, context(target, miEventRepository.findKeywordAlertMailEvent(target.miEventId())));
        mailService.sendMailWithInlineLogoNoAccessLog(target.userLoginId(), subject(target), body);
    }

    /** Figma: [AI Market Insights] 키워드 알림 발생 - {키워드} */
    static String subject(NotificationDispatchTarget target) {
        return "[AI Market Insights] 키워드 알림 발생 - %s".formatted(target.keywordName());
    }

    /**
     * 템플릿 변수 (th:text로 출력하므로 escape는 Thymeleaf가 처리함)
     * - eventMeta: 카드 1행 "신규 · 지정학적 리스크 · 2026-09-28 09:40" (이벤트 미존재 시 null → 행 생략)
     * - detailLink: 이벤트 상세 화면 링크 (Event List > 이벤트 상세)
     */
    Context context(NotificationDispatchTarget target, Optional<KeywordAlertMailEventRow> event) {
        Context context = new Context();
        context.setVariable("eventMeta", event.map(EmailNotificationDispatcher::eventMeta).orElse(null));
        context.setVariable("eventName", target.eventName());
        context.setVariable("keywordName", target.keywordName());
        context.setVariable("detailLink", serverDomain + EVENT_DETAIL_PATH.formatted(target.miEventId()));
        return context;
    }

    static String eventMeta(KeywordAlertMailEventRow event) {
        String status = Boolean.TRUE.equals(event.getIsNew()) ? "신규" : "갱신";
        String occurredAt = event.getOccurredAt() == null ? null : event.getOccurredAt().format(OCCURRED_AT_FORMAT);
        return String.join(" · ", Stream.of(status, event.getCategoryLabel(), occurredAt)
                .filter(StringUtils::isNotBlank)
                .toList());
    }

}
