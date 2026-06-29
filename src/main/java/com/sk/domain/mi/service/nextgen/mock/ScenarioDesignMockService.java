package com.sk.domain.mi.service.nextgen.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioConfirmationReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioBaseEventRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListItemRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.mi.dto.response.nextgen.design.DesignConfirmTable;
import com.sk.domain.mi.dto.response.nextgen.design.DesignEditTable;
import com.sk.domain.mi.dto.response.nextgen.design.DesignScenarioRow;
import com.sk.domain.mi.dto.response.nextgen.design.DesignTable;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStageRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStateRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDimensionTemplateRes;
import com.sk.domain.mi.service.nextgen.sse.ScenarioDesignSseRegistry;
import com.sk.domain.shared.enums.nextgen.ProbabilityDirection;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.enums.nextgen.SignpostStrength;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 시나리오 설계 mock 서비스 (인메모리 세션 시뮬레이터).
 * <p>
 * 공유 분기항목 템플릿(실DB 템플릿과 동일 데이터)으로 세션을 구성하고, 검토/Signpost 단계에서 SSE를 직접 emit해 Agent 콜백을 자가 시뮬레이션한다. (단일노드 mock이라 Redis pub/sub 대신 로컬 emit)
 */
@Slf4j
@Component
public class ScenarioDesignMockService {

    private static final long GROUP_ID_BASE = 900_000L;
    private static final long SCENARIO_ID_BASE = 950_000L;
    private static final long SIGNPOST_ID_BASE = 970_000L;

    private final ObjectMapper objectMapper;
    private final ScenarioDesignSseRegistry sseRegistry;
    private final NextGenMockProperties mockProps;
    private final ScenarioGroupMockService scenarioGroupMockService;
    private final AuthorizationService authorizationService;

    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicLong groupIdSeq = new AtomicLong(GROUP_ID_BASE);
    private final AtomicLong scenarioIdSeq = new AtomicLong(SCENARIO_ID_BASE);
    private final AtomicLong signpostIdSeq = new AtomicLong(SIGNPOST_ID_BASE);

    /** Agent 콜백 도착을 흉내내는 지연 스케줄러 (데몬). */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "design-mock-callback");
        t.setDaemon(true);
        return t;
    });

    /** event_type_l2_id → 분기항목 템플릿 (실DB 템플릿과 동일 데이터). */
    private Map<Long, ScenarioDimensionTemplateRes> templatesByEventType = Map.of();

    /** miEventId → event_type_l2_id (이벤트 실제 유형 매핑, events mock의 L1 기반). */
    private Map<Long, Long> eventTypeL2ByEvent = Map.of();

    public ScenarioDesignMockService(ObjectMapper objectMapper, ScenarioDesignSseRegistry sseRegistry,
                                     NextGenMockProperties mockProps, ScenarioGroupMockService scenarioGroupMockService,
                                     AuthorizationService authorizationService) {
        this.objectMapper = objectMapper;
        this.sseRegistry = sseRegistry;
        this.mockProps = mockProps;
        this.scenarioGroupMockService = scenarioGroupMockService;
        this.authorizationService = authorizationService;
    }

    @PostConstruct
    void init() {
        try (InputStream is = new ClassPathResource("mock/design/dimension-templates.json").getInputStream()) {
            templatesByEventType = objectMapper.readValue(is, new TypeReference<Map<Long, ScenarioDimensionTemplateRes>>() {});
        } catch (IOException e) {
            log.error("[Mock] Failed to load scenario design templates", e);
            templatesByEventType = Map.of();
        }
        try (InputStream is = new ClassPathResource("mock/design/event-l2-mapping.json").getInputStream()) {
            eventTypeL2ByEvent = objectMapper.readValue(is, new TypeReference<Map<Long, Long>>() {});
        } catch (IOException e) {
            log.warn("[Mock] Failed to load event→L2 mapping (fallback to hash): {}", e.getMessage());
            eventTypeL2ByEvent = Map.of();
        }
        log.info("[Mock] Scenario design mock loaded: eventTypeTemplates={}, eventL2Mapping={}",
            templatesByEventType.size(), eventTypeL2ByEvent.size());
    }

    // ===== API =====

    public ScenarioDesignStageRes startDesign(ScenarioDesignStartReq request) {
        // 점유 충돌: 같은 이벤트에 이미 설계 세션(진행/완료)이 있으면 거부 — 이벤트가 designable 상태가 아님.
        // (이로써 start ↔ 이벤트 설계가능 조회 ↔ 세션 상태가 유기적으로 일관된다)
        findSessionByEventId(request.miEventId()).ifPresent(existing -> {
            throw AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                "Event already has a design session: miEventId=%d, scenarioGroupId=%d"
                    .formatted(request.miEventId(), existing.scenarioGroupId()));
        });

        long groupId = groupIdSeq.incrementAndGet();
        Long eventTypeL2Id = pickEventType(request.miEventId());
        ScenarioDimensionTemplateRes template = templatesByEventType.get(eventTypeL2Id);

        List<ScenarioDimensionColumnRes> columns = buildColumns(template);
        List<DesignScenarioRow> step1 = generateInitialScenarios(template);

        // 점유자 = 설계를 시작한 실제 로그인 사용자(JWT user_no).
        Long occupantUserId = authorizationService.getUserNo().longValue();

        // 접수 시점은 ready(생성 대기). Agent 생성 콜백이 도착하면 scenario_created로 전이 + SSE.
        Session session = new Session(groupId, request.miEventId(), eventTypeL2Id, occupantUserId, columns, step1);
        session.stage = ScenarioGroupStage.READY;
        sessions.put(groupId, session);
        log.info("[Mock] design started: groupId={}, miEventId={}, eventTypeL2Id={}",
            groupId, request.miEventId(), eventTypeL2Id);

        scheduleAgentCallback(() -> {
            session.stage = ScenarioGroupStage.SCENARIO_CREATED;
            emit(groupId, ScenarioDesignSseEvent.Type.GENERATION, ScenarioDesignSseEvent.Stage.SCENARIO_CREATED);
        });
        return stage(session);
    }

    public ScenarioDesignStateRes getDesign(Long scenarioGroupId) {
        return buildState(getSession(scenarioGroupId));
    }

    /** 분기항목/분기기준 템플릿 조회 (mock 모드 — 세션의 event_type 실DB 템플릿 반환). */
    public ScenarioDimensionTemplateRes getDimensionTemplates(Long scenarioGroupId) {
        Session session = getSession(scenarioGroupId);
        ScenarioDimensionTemplateRes template = templatesByEventType.get(session.eventTypeL2Id);
        return template != null ? template : new ScenarioDimensionTemplateRes(List.of());
    }

    /** miEventId로 event_type 결정 — 이벤트 실제 유형 매핑 우선, 없으면 결정적 분배 폴백. */
    private Long pickEventType(Long miEventId) {
        if (templatesByEventType.isEmpty()) {
            return null;
        }
        Long mapped = miEventId == null ? null : eventTypeL2ByEvent.get(miEventId);
        if (mapped != null && templatesByEventType.containsKey(mapped)) {
            return mapped;
        }
        List<Long> ids = templatesByEventType.keySet().stream().sorted().toList();
        long key = miEventId == null ? 0L : miEventId;
        return ids.get((int) Math.floorMod(key, ids.size()));
    }

    /**
     * 이벤트의 설계 점유/상태 조회 (이벤트 설계가능 폴링 연동용).
     * 동일 이벤트로 여러 번 시작됐으면 가장 최근(그룹 ID 최대) 세션을 본다.
     */
    public Optional<DesignSessionInfo> findSessionByEventId(Long miEventId) {
        if (miEventId == null) {
            return Optional.empty();
        }
        return sessions.values().stream()
            .filter(s -> miEventId.equals(s.miEventId))
            .max(Comparator.comparingLong(s -> s.scenarioGroupId))
            .map(s -> new DesignSessionInfo(s.scenarioGroupId, s.status, s.occupantUserId));
    }

    /** 이벤트 설계가능 폴링이 참조하는 세션 요약. */
    public record DesignSessionInfo(Long scenarioGroupId, ScenarioGroupStatus status, Long occupantUserId) {}

    public ScenarioDesignStageRes validate(Long scenarioGroupId, ScenarioValidationReq request) {
        Session session = getSession(scenarioGroupId);
        applyEdit(session, request.table());
        // 접수 시점은 stage 유지(scenario_created). 검토 콜백 도착 시 결과에 따라 전이 + SSE.
        // mock 한정: 50% 확률로 검증 실패를 시뮬레이션
        scheduleAgentCallback(() -> {
            boolean valid = ThreadLocalRandom.current().nextBoolean();
            session.validationIsValid = valid;
            if (valid) {
                session.validationReason = "MECE 및 타당성 검증 통과 (mock)";
                session.stage = ScenarioGroupStage.SCENARIO_VALIDATED;
                emit(scenarioGroupId, ScenarioDesignSseEvent.Type.VALIDATION, ScenarioDesignSseEvent.Stage.SCENARIO_VALIDATED);
            } else {
                session.validationReason = pickMockValidationFailReason();
                // 실패 시 stage 미전이(scenario_created 유지) → 재편집 후 재검증 가능
                emit(scenarioGroupId, ScenarioDesignSseEvent.Type.VALIDATION, ScenarioDesignSseEvent.Stage.SCENARIO_CREATED);
            }
        });
        return stage(session);
    }

    public ScenarioDesignStageRes createSignposts(Long scenarioGroupId, ScenarioSignpostsReq request) {
        Session session = getSession(scenarioGroupId);
        applyEdit(session, request.table());
        // 접수 시점은 stage 유지(scenario_validated). Signpost 콜백 도착 시 signpost_created 전이 + SSE.
        scheduleAgentCallback(() -> {
            session.step2 = generateOneView(session.step1);
            session.stage = ScenarioGroupStage.SIGNPOST_CREATED;
            session.validationIsValid = null;
            session.validationReason = null;
            emit(scenarioGroupId, ScenarioDesignSseEvent.Type.SIGNPOSTS, ScenarioDesignSseEvent.Stage.SIGNPOST_CREATED);
        });
        return stage(session);
    }

    /** Agent 콜백 도착을 흉내내 지연 실행한다. 지연 0이면 동기 실행(테스트/즉시 모드). */
    private void scheduleAgentCallback(Runnable task) {
        long delaySeconds = mockProps.getDesign().getCallbackDelaySeconds();
        if (delaySeconds <= 0) {
            task.run();
            return;
        }
        scheduler.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("[Mock] design agent-callback simulation failed: {}", e.getMessage());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    public ScenarioDesignStageRes confirm(Long scenarioGroupId, ScenarioConfirmationReq request) {
        Session session = getSession(scenarioGroupId);
        if (request.scenarioGroup() != null) {
            session.groupName = request.scenarioGroup().title();
            session.summary = request.scenarioGroup().summary();
            session.description = request.scenarioGroup().description();
        }
        session.step2 = applyConfirm(request.table());
        session.status = ScenarioGroupStatus.CREATED;
        session.stage = ScenarioGroupStage.COMPLETED;
        session.validationIsValid = null;
        session.validationReason = null;
        materializeGroup(session); // 확정된 그룹을 그룹 도메인에 등록 → 목록/상세/개별상세 노출
        log.info("[Mock] design confirmed: groupId={}", scenarioGroupId);
        return stage(session);
    }

    /** 확정된 설계 세션(step2)을 그룹 도메인 DTO로 변환해 그룹 mock에 등록한다. */
    private void materializeGroup(Session session) {
        Long groupId = session.scenarioGroupId;
        List<DesignScenarioRow> rows = session.step2 == null ? List.of() : session.step2;
        LocalDateTime now = LocalDateTime.now();

        List<ScenarioRowRes> listRows = rows.stream()
            .map(r -> new ScenarioRowRes(r.scenarioId(), r.scenarioName(), r.baseCase(), r.displayOrder(),
                r.dimensions(), r.probability(), r.probability(),
                ScenarioDisplayStatus.PENDING, ScenarioDisplayStatus.PENDING.getLabel()))
            .toList();
        ScenarioGroupListItemRes listItem = new ScenarioGroupListItemRes(
            groupId, session.groupName, now, now, session.summary, rows.size(), session.columns, listRows);

        ScenarioBaseEventRes baseEvent = new ScenarioBaseEventRes(session.miEventId, null, null, null, now);
        List<ScenarioGroupDetailRowRes> detailRows = rows.stream()
            .map(r -> new ScenarioGroupDetailRowRes(r.scenarioId(), r.scenarioName(), r.baseCase(), r.displayOrder(),
                r.dimensions(), r.probability(), r.probability(),
                ScenarioDisplayStatus.PENDING, ScenarioDisplayStatus.PENDING.getLabel()))
            .toList();
        ScenarioGroupDetailRes detail = new ScenarioGroupDetailRes(
            groupId, session.groupName, now, now, baseEvent, session.summary, rows.size(), session.columns, detailRows);

        List<ScenarioDetailViewRes> scenarioDetails = rows.stream()
            .map(r -> new ScenarioDetailViewRes(
                r.scenarioId(), groupId, r.scenarioName(), r.baseCase(), r.displayOrder(), now, now,
                r.narrative(), r.probability(), r.probability(), r.dimensions(),
                r.signposts() == null ? List.of() : r.signposts(), List.of()))
            .toList();

        scenarioGroupMockService.registerGroup(listItem, detail, scenarioDetails);
    }

    public ScenarioDesignStateRes reset(Long scenarioGroupId) {
        Session session = getSession(scenarioGroupId);
        session.columns = session.initialColumns;
        session.step1 = session.initialStep1;
        session.step2 = null;
        session.validationIsValid = null;
        session.validationReason = null;
        session.status = ScenarioGroupStatus.CREATING;
        session.stage = ScenarioGroupStage.SCENARIO_CREATED;
        return buildState(session);
    }

    /**
     * [mock 전용] 설계 세션 제거 (그룹 삭제/취소 연동).
     * <p>세션이 사라지면 해당 이벤트의 설계가능 판정이 다시 '설계가능'으로 풀린다(이벤트↔설계 유기적 연동).
     * @return 제거된 세션이 있었으면 true
     */
    public boolean removeSessionByGroupId(Long scenarioGroupId) {
        return scenarioGroupId != null && sessions.remove(scenarioGroupId) != null;
    }

    /** [mock 전용] 모든 설계 세션과 채번 카운터를 초기 상태로 되돌린다 (시나리오 상태/삭제 초기화 API에서 호출). */
    public void resetSessions() {
        sessions.clear();
        groupIdSeq.set(GROUP_ID_BASE);
        scenarioIdSeq.set(SCENARIO_ID_BASE);
        signpostIdSeq.set(SIGNPOST_ID_BASE);
    }

    // ===== 내부 =====

    private Session getSession(Long scenarioGroupId) {
        Session session = sessions.get(scenarioGroupId);
        if (session == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario design session not found: " + scenarioGroupId);
        }
        return session;
    }

    private ScenarioDesignStageRes stage(Session s) {
        return ScenarioDesignStageRes.of(s.scenarioGroupId, s.status, s.stage);
    }

    private ScenarioDesignStateRes buildState(Session s) {
        boolean step1Stage = s.stage == ScenarioGroupStage.SCENARIO_CREATED
            || s.stage == ScenarioGroupStage.SCENARIO_VALIDATED;
        boolean step2Stage = s.stage == ScenarioGroupStage.SIGNPOST_CREATED
            || s.stage == ScenarioGroupStage.COMPLETED;

        DesignTable step1 = step1Stage ? DesignTable.of(s.columns, s.step1) : null;
        ScenarioDesignStateRes.OneView step2 = step2Stage
            ? ScenarioDesignStateRes.OneView.of(s.groupName, s.summary, s.description, DesignTable.of(s.columns, s.step2))
            : null;
        ScenarioDesignStateRes.Validation validation = s.validationIsValid == null
            ? null
            : ScenarioDesignStateRes.Validation.of(s.validationIsValid, s.validationReason);

        return ScenarioDesignStateRes.of(
            s.scenarioGroupId, s.status, s.stage, s.occupantUserId != null, s.occupantUserId,
            step1, step2, validation);
    }

    private void emit(Long groupId, ScenarioDesignSseEvent.Type type, ScenarioDesignSseEvent.Stage stage) {
        sseRegistry.emit(groupId, ScenarioDesignSseEvent.of(groupId, type, stage));
    }

    /** 편집 표(검토/Signpost 요청)를 세션 step1에 반영. baseCase는 기존 행 기준으로 보존. */
    private void applyEdit(Session session, DesignEditTable table) {
        Set<Long> baseCaseIds = session.step1.stream()
            .filter(DesignScenarioRow::baseCase)
            .map(DesignScenarioRow::scenarioId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());

        List<DesignScenarioRow> rows = new ArrayList<>();
        for (var r : table.scenarios()) {
            Long scenarioId = r.scenarioId() != null ? r.scenarioId() : scenarioIdSeq.incrementAndGet();
            rows.add(DesignScenarioRow.of(
                scenarioId, r.displayOrder(), r.scenarioName(),
                baseCaseIds.contains(scenarioId), r.dimensions(), null, null, null));
        }
        session.columns = table.dimensionColumns();
        session.step1 = List.copyOf(rows);
    }

    /** 확정 표를 step2(full)로 반영. */
    private List<DesignScenarioRow> applyConfirm(DesignConfirmTable table) {
        List<DesignScenarioRow> rows = new ArrayList<>();
        for (var r : table.scenarios()) {
            List<ScenarioSignpostDefinitionViewRes> signposts = r.signposts() == null ? List.of()
                : r.signposts().stream()
                    .map(sp -> ScenarioSignpostDefinitionViewRes.of(
                        sp.signpostId(), sp.label(), sp.direction(), null, sp.strength(), sp.rationale(), sp.displayOrder()))
                    .toList();
            rows.add(DesignScenarioRow.of(
                r.scenarioId(), r.displayOrder(), r.scenarioName(), r.baseCase(),
                r.dimensions(), r.probability(), r.narrative(), signposts));
        }
        return List.copyOf(rows);
    }

    private List<ScenarioDimensionColumnRes> buildColumns(ScenarioDimensionTemplateRes template) {
        if (template == null) {
            return List.of();
        }
        return sessionDimensions(template).stream()
            .map(d -> new ScenarioDimensionColumnRes(d.dimKey(), d.label()))
            .toList();
    }

    /**
     * 샘플 세션이 보유할 분기항목 부분집합(필수 항목만, displayOrder 순).
     * <p>템플릿 조회 API는 전체 분기항목을 내려주므로, 세션은 일부만 들고 있어야
     * '템플릿에서 분기항목 추가' 액션을 FE가 테스트할 수 있다.
     * 필수가 없거나 전부 필수면 테스트 여지 확보를 위해 앞쪽 2개만 사용한다.
     */
    private List<ScenarioDimensionTemplateRes.Item> sessionDimensions(ScenarioDimensionTemplateRes template) {
        List<ScenarioDimensionTemplateRes.Item> all = template.dimensions().stream()
            .sorted(Comparator.comparing(d -> d.displayOrder() == null ? Integer.MAX_VALUE : d.displayOrder()))
            .toList();
        List<ScenarioDimensionTemplateRes.Item> required = all.stream()
            .filter(d -> Boolean.TRUE.equals(d.required()))
            .toList();
        if (required.isEmpty() || required.size() == all.size()) {
            return all.subList(0, Math.min(2, all.size()));
        }
        return required;
    }

    /** mock 검증 실패 시 내려줄 장난스러운 사유 풀 (mock 전용). */
    private static final List<String> MOCK_VALIDATION_FAIL_REASONS = List.of(
        "시나리오들이 서로 너무 사이좋아서 MECE가 깨졌습니다. 조금 다투게 해주세요 (mock)",
        "3번 시나리오가 '저는 빼주세요'라며 손을 들었습니다. 상호배타성 위반 (mock)",
        "확률 합이 100을 살짝 넘봤습니다. 욕심이 과했네요 (mock)",
        "Agent가 박재현이랑 커피 마시다 검증 기준 한 줄을 놓쳤습니다. 다시 제출 부탁드려요 (mock)",
        "분기기준값이 너무 애매해서 Agent가 고개를 갸웃거렸습니다 (mock)",
        "시나리오 네이밍이 비장한데 내용이 평화로워서 톤 검증 실패 (mock)"
    );

    /** 실패 사유를 무작위로 하나 고른다 (mock 전용). */
    private String pickMockValidationFailReason() {
        return MOCK_VALIDATION_FAIL_REASONS.get(
            ThreadLocalRandom.current().nextInt(MOCK_VALIDATION_FAIL_REASONS.size()));
    }

    private List<DesignScenarioRow> generateInitialScenarios(ScenarioDimensionTemplateRes template) {
        if (template == null) {
            return List.of();
        }
        List<ScenarioDimensionTemplateRes.Item> dimensions = sessionDimensions(template);
        String[] titles = {"단기 외교적 해소", "공급 차질 장기화", "전면 봉쇄 심화", "점진적 정상화"};
        List<DesignScenarioRow> rows = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            List<ScenarioDimensionRes> dims = new ArrayList<>();
            for (var dim : dimensions) {
                List<String> enums = dim.valueEnums();
                String value = (enums == null || enums.isEmpty()) ? null : enums.get(i % enums.size());
                dims.add(new ScenarioDimensionRes(dim.dimKey(), dim.label(), value, value));
            }
            long scenarioId = scenarioIdSeq.incrementAndGet();
            rows.add(DesignScenarioRow.of(scenarioId, i + 1, titles[i], i == 0, dims, null, null, null));
        }
        return List.copyOf(rows);
    }

    /** step1 → One-view(step2): 시나리오별 narrative/확률/Signpost 산출 시뮬레이션. */
    private List<DesignScenarioRow> generateOneView(List<DesignScenarioRow> step1) {
        int[] probs = {55, 25, 12, 8};
        List<DesignScenarioRow> rows = new ArrayList<>();
        for (int i = 0; i < step1.size(); i++) {
            DesignScenarioRow row = step1.get(i);
            Integer probability = i < probs.length ? probs[i] : 5;
            String narrative = "%s 경로가 전개되는 시나리오. 관측 가능한 사건(Signpost)에 따라 발생 확률이 갱신된다.".formatted(row.scenarioName());
            rows.add(DesignScenarioRow.of(
                row.scenarioId(), row.displayOrder(), row.scenarioName(), row.baseCase(),
                row.dimensions(), probability, narrative, generateSignposts(i)));
        }
        return List.copyOf(rows);
    }

    private List<ScenarioSignpostDefinitionViewRes> generateSignposts(int scenarioIdx) {
        String[][] pool = {
            {"호르무즈 해협 통항 제한 장기화", "운송 리스크 확대 및 대체 항로 비용 증가"},
            {"주요 LNG 수출국 생산 차질 공식 발표", "단기 공급 타이트 심화"},
            {"유럽 가스 재고 평년 대비 급감", "동북아·유럽 수요측 확보 경쟁 격화"},
            {"대체 항로 운임 급등", "물류 비용 상방 압력 확대"}
        };
        SignpostStrength[] strengths = {
            SignpostStrength.STRONG, SignpostStrength.MEDIUM, SignpostStrength.VERYSTRONG, SignpostStrength.WEAK};

        List<ScenarioSignpostDefinitionViewRes> list = new ArrayList<>();
        for (int k = 0; k < 2; k++) {
            int idx = (scenarioIdx + k) % pool.length;
            long signpostId = signpostIdSeq.incrementAndGet();
            int impact = 80 - idx * 15;
            list.add(ScenarioSignpostDefinitionViewRes.of(
                signpostId, pool[idx][0], ProbabilityDirection.SUP, impact,
                strengths[idx % strengths.length], pool[idx][1], k + 1));
        }
        return list;
    }

    /** 설계 세션 (mock 인메모리). */
    private static final class Session {
        final Long scenarioGroupId;
        final Long miEventId;
        final Long eventTypeL2Id;
        final Long occupantUserId;
        final List<ScenarioDimensionColumnRes> initialColumns;
        final List<DesignScenarioRow> initialStep1;

        ScenarioGroupStatus status = ScenarioGroupStatus.CREATING;
        ScenarioGroupStage stage = ScenarioGroupStage.SCENARIO_CREATED;
        List<ScenarioDimensionColumnRes> columns;
        List<DesignScenarioRow> step1;
        List<DesignScenarioRow> step2;
        Boolean validationIsValid;
        String validationReason;
        String groupName = "(설계중) 호르무즈 해협 봉쇄 시나리오 그룹";
        String summary = "호르무즈 해협 통항 제한에 따른 LNG 공급 차질 시나리오 (mock)";
        String description = "Agent 생성 초기값 기반 설계 세션 (mock)";

        Session(Long scenarioGroupId, Long miEventId, Long eventTypeL2Id, Long occupantUserId,
                List<ScenarioDimensionColumnRes> columns, List<DesignScenarioRow> step1) {
            this.scenarioGroupId = scenarioGroupId;
            this.miEventId = miEventId;
            this.eventTypeL2Id = eventTypeL2Id;
            this.occupantUserId = occupantUserId;
            this.initialColumns = columns;
            this.initialStep1 = step1;
            this.columns = columns;
            this.step1 = step1;
        }
    }

}
