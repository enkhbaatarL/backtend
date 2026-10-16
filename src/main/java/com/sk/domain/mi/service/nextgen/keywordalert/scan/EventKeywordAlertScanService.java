package com.sk.domain.mi.service.nextgen.keywordalert.scan;

import com.sk.domain.shared.entity.master.tmd.EventKeywordSetting;
import com.sk.domain.shared.repository.master.tmd.EventKeywordSettingRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.repository.master.tmm.projection.KeywordAlertEventRow;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 이벤트 키워드 알림 스캔 (Redis 락을 잡은 1개 노드만 실행)
 * - 대상: 생성·갱신된 이벤트 행
 * - 기준 컬럼: mod_dtm (생성 시에도 채워지므로 신규·갱신을 함께 판단)
 *   - latest_record_first_created: 레코드 시각이라 적재보다 과거 → 누락 위험, 사용 안 함
 *   - 이벤트·키워드당 알림은 1회 — 이미 알림이 있는 조합은 갱신돼도 재발송하지 않음 (Writer 존재 확인 + uq01)
 * - 체크포인트: 마지막 처리 이벤트의 mod_dtm (Redis). 없으면 현재 최대 mod_dtm으로 초기화 → 기존 이벤트 무시
 * - 재스캔: 체크포인트 2분 전부터 조회 (늦은 커밋 대비). 중복 알림은 Writer 존재 확인 + uq01로 차단
 * - 실패 시: 체크포인트 유지 → 다음 스캔에서 재처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventKeywordAlertScanService {

    static final Duration OVERLAP = Duration.ofMinutes(2);

    /** 대상 이벤트가 없을 때의 체크포인트 초기값 (이후 생성되는 첫 이벤트부터 알림 대상) */
    static final LocalDateTime EMPTY_TABLE_CHECKPOINT = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final EventKeywordAlertScanLockStore scanLockStore;
    private final EventKeywordAlertCheckpointStore checkpointStore;
    private final MiEventRepository miEventRepository;
    private final EventKeywordSettingRepository eventKeywordSettingRepository;
    private final EventKeywordMatcher eventKeywordMatcher;
    private final EventKeywordNotificationWriter eventKeywordNotificationWriter;

    /**
     * [키워드 알림 스캔 1회]
     * - 다른 노드가 락을 잡고 있으면 아무것도 하지 않음
     * - 트랜잭션은 이벤트 단위(Writer)로만 엶. 스캔 전체를 한 트랜잭션으로 묶으면 한 건 실패에 전부 롤백되고 락 보유 중 커넥션을 오래 잡음
     */
    public void scan() {
        Optional<String> token = scanLockStore.tryAcquire();
        if (token.isEmpty()) {
            return;
        }
        try {
            scanWithLock();
        } finally {
            scanLockStore.release(token.get());
        }
    }

    private void scanWithLock() {
        Optional<LocalDateTime> checkpoint = checkpointStore.get();
        if (checkpoint.isEmpty()) {
            LocalDateTime initial = miEventRepository.findMaxModifyDt().orElse(EMPTY_TABLE_CHECKPOINT);
            checkpointStore.save(initial);
            log.info("[Keyword Alert Scan] checkpoint initialized (existing events ignored): {}", initial);
            return;
        }

        // OVERLAP: 체크포인트보다 2분 앞에서부터 다시 읽어, 늦게 커밋된(mod_dtm이 체크포인트 이전인) 이벤트를 놓치지 않음
        List<KeywordAlertEventRow> events = miEventRepository.findKeywordAlertEventsModifiedAfter(checkpoint.get().minus(OVERLAP));
        if (events.isEmpty()) {
            return;
        }

        List<EventKeywordSetting> settings = eventKeywordSettingRepository.findAllMatchable();
        Map<Long, List<EventKeywordSetting>> matches = eventKeywordMatcher.match(settings, events);

        int created = 0;
        boolean failed = false;
        for (KeywordAlertEventRow event : events) {
            List<EventKeywordSetting> matched = matches.get(event.getMiEventId());
            if (matched == null) {
                continue;
            }
            try {
                created += eventKeywordNotificationWriter.write(event, matched);
            } catch (RuntimeException e) {
                failed = true;
                log.error("[Keyword Alert Scan] notification write failed: miEventId={}", event.getMiEventId(), e);
            }
        }

        LocalDateTime latest = events.get(events.size() - 1).getModifyDt();
        if (!failed && latest.isAfter(checkpoint.get())) {
            checkpointStore.save(latest);
        }
        log.info("[Keyword Alert Scan] events={}, keywordSettings={}, matchedEvents={}, notificationsCreated={}, failed={}, checkpoint={}",
                events.size(), settings.size(), matches.size(), created, failed, failed ? checkpoint.get() : latest);
    }

}
