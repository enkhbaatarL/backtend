package com.sk.domain.mi.service.nextgen.support;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotCompareRes.FieldDifference;
import com.sk.domain.shared.entity.master.tmm.MiEvent;
import com.sk.domain.shared.entity.master.tmm.MiEventSnapshot;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.EventSignalType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * [MCP Snapshot] Event Snapshot 비교. 비교 필드는 이 클래스의 FIELDS 한 곳에서만 정의한다.
 * 두 상태를 직접 비교하며 changed_fields_json(파이프라인 기록값)은 쓰지 않는다.
 */
public final class EventSnapshotDiffCalculator {

    private static final List<Field> FIELDS = List.of(
        new Field("eventName", "event_title", State::eventName),
        new Field("oneLineSummary", "one_line_summary", State::oneLineSummary),
        new Field("keySummaries", "key_bullets_json", State::keySummaries),
        new Field("detailedAnalysis", "detail_analysis", State::detailedAnalysis),
        new Field("eventTypeL2Id", "event_type_l2_id", State::eventTypeL2Id),
        new Field("tags", "tags_json", State::tags),
        new Field("estimatedImpactId", "estimated_impact_id", State::estimatedImpactId),
        new Field("signalType", "signal_yn", State::signalType),
        new Field("signalConfidence", "signal_confidence", State::signalConfidence),
        new Field("signalReason", "signal_reason", State::signalReason),
        new Field("isScenarioSuggested", "scenario_suggest_yn", State::isScenarioSuggested),
        new Field("scenarioSuggestReason", "scenario_suggest_reason", State::scenarioSuggestReason),
        new Field("status", "new_yn", State::status),
        new Field("isEventDeleted", "event_del_yn", State::isEventDeleted),
        new Field("institutionViewSynthesis", "institution_view_synthesis", State::institutionViewSynthesis),
        new Field("occurredAt", "occurred_at", State::occurredAt),
        new Field("latestRecordFirstCreated", "latest_record_first_created", State::latestRecordFirstCreated),
        new Field("oldestRecordFirstCreated", "oldest_record_first_created", State::oldestRecordFirstCreated)
    );

    private EventSnapshotDiffCalculator() {
    }

    public static Result compare(State base, State compared) {
        List<FieldDifference> differences = new ArrayList<>();
        List<String> sameFields = new ArrayList<>();
        for (Field field : FIELDS) {
            Object baseValue = field.getter().apply(base);
            Object comparedValue = field.getter().apply(compared);
            if (isSame(baseValue, comparedValue)) {
                sameFields.add(field.name());
            } else {
                differences.add(new FieldDifference(field.name(), field.column(), baseValue, comparedValue));
            }
        }
        return new Result(differences, sameFields);
    }

    // BigDecimal 은 scale 이 달라도 값이 같으면 같다 (0.5 == 0.5000)
    private static boolean isSame(Object left, Object right) {
        if (left instanceof BigDecimal l && right instanceof BigDecimal r) {
            return l.compareTo(r) == 0;
        }
        return Objects.equals(left, right);
    }

    public record Result(List<FieldDifference> differences, List<String> sameFields) {
    }

    private record Field(String name, String column, Function<State, Object> getter) {
    }

    /**
     * 비교 한쪽의 상태. JSON 은 파싱한 값, 시각은 응답과 같은 KST 로 담는다.
     */
    public record State(
        String eventName,
        String oneLineSummary,
        List<String> keySummaries,
        String detailedAnalysis,
        Long eventTypeL2Id,
        List<String> tags,
        String estimatedImpactId,
        EventSignalType signalType,
        BigDecimal signalConfidence,
        String signalReason,
        Boolean isScenarioSuggested,
        String scenarioSuggestReason,
        ContentStatus status,
        Boolean isEventDeleted,
        String institutionViewSynthesis,
        LocalDateTime occurredAt,
        LocalDateTime latestRecordFirstCreated,
        LocalDateTime oldestRecordFirstCreated
    ) {

        public static State of(MiEventSnapshot snapshot) {
            return new State(
                snapshot.getEventTitle(),
                snapshot.getOneLineSummary(),
                JsonUtil.parseWrappedStringList(snapshot.getKeyBulletsJson(), "bullets"),
                snapshot.getDetailAnalysis(),
                snapshot.getEventTypeL2Id(),
                JsonUtil.parseWrappedStringList(snapshot.getTagsJson(), "tags"),
                snapshot.getEstimatedImpactId(),
                signalType(snapshot.getIsSignal()),
                snapshot.getSignalConfidence(),
                snapshot.getSignalReason(),
                Boolean.TRUE.equals(snapshot.getIsScenarioSuggested()),
                snapshot.getScenarioSuggestReason(),
                status(snapshot.getIsNew()),
                Boolean.TRUE.equals(snapshot.getIsEventDeleted()),
                snapshot.getInstitutionViewSynthesis(),
                DateUtil.utcToKst(snapshot.getOccurredAt()),
                DateUtil.utcToKst(snapshot.getLatestRecordFirstCreated()),
                DateUtil.utcToKst(snapshot.getOldestRecordFirstCreated()));
        }

        /** 현재 Event. eventTypeL2 는 LAZY 연관이라 id 만 꺼낸다. 삭제 여부는 원본 del_yn. */
        public static State of(MiEvent event) {
            return new State(
                event.getEventTitle(),
                event.getOneLineSummary(),
                JsonUtil.parseWrappedStringList(event.getKeyBulletsJson(), "bullets"),
                event.getDetailAnalysis(),
                event.getEventTypeL2() == null ? null : event.getEventTypeL2().getEventTypeL2Id(),
                JsonUtil.parseWrappedStringList(event.getTagsJson(), "tags"),
                event.getEstimatedImpactId(),
                signalType(event.getIsSignal()),
                event.getSignalConfidence(),
                event.getSignalReason(),
                Boolean.TRUE.equals(event.getIsScenarioSuggested()),
                event.getScenarioSuggestReason(),
                status(event.getIsNew()),
                Boolean.TRUE.equals(event.getIsDeleted()),
                event.getInstitutionViewSynthesis(),
                DateUtil.utcToKst(event.getOccurredAt()),
                DateUtil.utcToKst(event.getLatestRecordFirstCreated()),
                DateUtil.utcToKst(event.getOldestRecordFirstCreated()));
        }

        private static EventSignalType signalType(Boolean isSignal) {
            return Boolean.TRUE.equals(isSignal) ? EventSignalType.SIGNAL : EventSignalType.NOISE;
        }

        private static ContentStatus status(Boolean isNew) {
            return Boolean.TRUE.equals(isNew) ? ContentStatus.NEW : ContentStatus.UPDATED;
        }
    }

}
