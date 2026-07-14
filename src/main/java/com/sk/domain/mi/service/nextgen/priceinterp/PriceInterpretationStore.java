package com.sk.domain.mi.service.nextgen.priceinterp;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.priceinterp.PriceInterpretationGenerationCallbackReq;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationItemRes;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSectionRes;
import com.sk.domain.shared.entity.master.tmm.MiScenarioPriceInterp;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioLatestRunRow;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioPriceInterpRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가격전망 해석 영속 계층 전담
 * Agent 호출(네트워크)과 DB 트랜잭션을 분리하기 위해 영속화를 이 컴포넌트에 집약시켜둠
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceInterpretationStore {

    private final MiScenarioGroupRepository scenarioGroupRepository;
    private final MiScenarioRepository scenarioRepository;
    private final MiScenarioPriceInterpRepository priceInterpRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;

    /**
     * [경량 가격전망해석 조회]
     * 그룹 존재 검증 후 시나리오를 로드하여 조립한다.
     * SSE 신호 수신 시 FE가 그룹 상세 대신 재조회하는 대상.
     */
    public PriceInterpretationSectionRes readPriceInterpretations(Long scenarioGroupId) {
        requireActiveGroup(scenarioGroupId);
        return buildSection(scenarioGroupId, scenarioGroupRepository.findScenariosByGroupId(scenarioGroupId));
    }

    /**
     * [경량 가격전망해석 조립]
     * 이미 로드된 시나리오로 section을 조립한다 (시나리오 그룹 상세에서 재조회 없이 재사용).
     * - generatable(생성 버튼 활성) = 그룹 내에 (수급모델 실행 완료 AND (해석 미보유 OR needsRegeneration))인 시나리오 존재.
     */
    public PriceInterpretationSectionRes buildSection(Long scenarioGroupId, List<ScenarioBaseRow> scenarios) {

        List<Long> scenarioIds = scenarios.stream().map(ScenarioBaseRow::scenarioId).toList();

        Map<Long, String> latestRunByScenario = latestRunByScenario(scenarioIds);

        List<MiScenarioPriceInterp> scenarioInterps = priceInterpRepository.findByScenarioGroupId(scenarioGroupId);

        Map<Long, MiScenarioPriceInterp> byScenarioId = byScenarioId(scenarioInterps);

        // 시나리오를 순회하며 가격전망해석 생성여부를 판단
        boolean generatable = scenarios.stream().anyMatch(scenario -> {
            String latestRunId = latestRunByScenario.get(scenario.scenarioId());
            if (latestRunId == null) return false; // 완료된 수급모델 실행 없음 -> 생성 대상 아님
            MiScenarioPriceInterp row = byScenarioId.get(scenario.scenarioId());
            return row == null || row.needsRegeneration(latestRunId);
        });

        List<PriceInterpretationItemRes> items = scenarios.stream()
            .map(scenario -> toItem(scenario, byScenarioId.get(scenario.scenarioId())))
            .filter(Objects::nonNull)
            .toList();

        return PriceInterpretationSectionRes.of(generatable, items);
    }

    /**
     * 생성중/생성완료는 표시, 실패(만료된 생성중 포함)는 직전 해석이 남아 있는 경우(재실행 실패)만 표시
     */
    private static PriceInterpretationItemRes toItem(ScenarioBaseRow scenario, MiScenarioPriceInterp interp) {
        if (interp == null) return null;
        boolean visible = switch (interp.displayStatus()) {
            case GENERATING, GENERATED -> true;
            case FAILED -> interp.getCurrentContent() != null;
        };
        return visible ? PriceInterpretationItemRes.of(scenario, interp) : null;
    }

    /**
     * [생성 시작]
     * 대상 시나리오를 generating으로 생성/전이하고 그 시나리오 ID 목록을 반환한다.
     * 대상 = 수급모델 실행 완료(최신 완료 run 존재) AND (해석 미보유 OR needsRegeneration: 실패/만료/재실행 stale).
     */
    @Transactional
    public List<Long> beginGeneration(Long scenarioGroupId) {

        requireActiveGroup(scenarioGroupId);

        List<ScenarioBaseRow> scenarios = scenarioGroupRepository.findScenariosByGroupId(scenarioGroupId);
        List<Long> scenarioIds = scenarios.stream().map(ScenarioBaseRow::scenarioId).toList();

        Map<Long, String> latestRunByScenario = latestRunByScenario(scenarioIds);

        List<MiScenarioPriceInterp> interps = priceInterpRepository.findByScenario_ScenarioIdInAndIsDeletedFalse(scenarioIds);

        Map<Long, MiScenarioPriceInterp> existing = byScenarioId(interps);

        List<Long> targets = new ArrayList<>();
        for (ScenarioBaseRow scenario : scenarios) {
            Long scenarioId = scenario.scenarioId();
            String latestRunId = latestRunByScenario.get(scenarioId);

            if (latestRunId == null) {
                continue; // 완료된 수급모델 실행 없음 -> 생성 대상 아님
            }

            MiScenarioPriceInterp row = existing.get(scenarioId);

            if (row == null) {
                MiScenarioPriceInterp created = MiScenarioPriceInterp.toEntity(scenarioRepository.getReferenceById(scenarioId));
                created.updateRunId(latestRunId);
                priceInterpRepository.save(created);
                targets.add(scenarioId);
            } else if (row.needsRegeneration(latestRunId)) {
                row.markGenerating(); // 실패 재시도 / 만료 복구 / 재실행(stale) 재생성
                row.updateRunId(latestRunId);
                targets.add(scenarioId);
            }

            // 정상 생성중이거나 최신 run 기준 해석 보유 -> skip
        }

        if (targets.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "No target scenarios to generate price interpretation for.");
        }

        return targets;

    }

    /**
     * [생성 실패 처리] 대상 시나리오 중 아직 generating인 건을 failed로 전이한다 (Agent 접수 실패 시 복구).
     */
    @Transactional
    public void markFailed(Collection<Long> scenarioIds, String reason) {
        if (scenarioIds == null || scenarioIds.isEmpty()) return;
        priceInterpRepository.findByScenario_ScenarioIdInAndIsDeletedFalse(scenarioIds).stream()
            .filter(MiScenarioPriceInterp::isGenerating)
            .forEach(row -> row.applyFailed(reason));
    }

    /**
     * [생성 콜백 적재]
     * Agent 결과를 시나리오별로 적재
     */
    @Transactional
    public void applyGenerationCallback(Long scenarioGroupId, PriceInterpretationGenerationCallbackReq request) {

        List<MiScenarioPriceInterp> scenarioPriceInterps = priceInterpRepository.findByScenarioGroupId(scenarioGroupId);

        Map<Long, MiScenarioPriceInterp> byScenario = byScenarioId(scenarioPriceInterps);

        // 실패 응답
        if (!request.isSuccess()) {
            byScenario.values().stream()
                .filter(MiScenarioPriceInterp::isGenerating)
                .forEach(row -> row.applyFailed(request.reason()));
            log.warn("[PriceInterp Callback] agent job failed: groupId={}, reason={}", scenarioGroupId, request.reason());
            return;
        }

        if (request.interpretations() == null) {
            return;
        }

        for (PriceInterpretationGenerationCallbackReq.Item item : request.interpretations()) {
            MiScenarioPriceInterp row = byScenario.get(item.scenarioId());

            if (row == null) {
                log.warn("[PriceInterp Callback] interpretation for unknown scenario skipped: groupId={}, scenarioId={}", scenarioGroupId, item.scenarioId());
                continue;
            }

            if (!row.isGenerating()) {
                log.warn("[PriceInterp Callback] non-generating row skipped: groupId={}, scenarioId={}, status={}", scenarioGroupId, item.scenarioId(), row.getStatus());
                continue;
            }

            row.applyGenerated(item.content());
        }

    }

    /**
     * [수정]
     * 가격전망 해석을 수정한다.
     */
    @Transactional
    public MiScenarioPriceInterp edit(Long scenarioGroupId, Long scenarioId, String content) {
        MiScenarioPriceInterp row = load(scenarioGroupId, scenarioId);
        if (row.isGenerationInProgress()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Cannot edit price interpretation while generation is in progress.");
        }
        row.edit(content);
        return row;
    }

    /**
     * [초기화]
     * 가격전망 해석을 에이전트 초기 응답으로 초기화한다.
     */
    @Transactional
    public MiScenarioPriceInterp reset(Long scenarioGroupId, Long scenarioId) {
        MiScenarioPriceInterp row = load(scenarioGroupId, scenarioId);
        if (row.isGenerationInProgress()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "Cannot reset price interpretation while generation is in progress.");
        }
        if (!row.isResettable()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "No agent-generated original content to reset to.");
        }
        row.reset();
        return row;
    }

    /**
     * [수급모델 실행 전 무효화]
     * 최종 실행 등록 transaction에서 대상 시나리오의 활성 가격전망 해석만 soft delete 한다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void softDeleteForModelExecution(Long scenarioId) {
        priceInterpRepository.findActiveByScenarioIdForUpdate(scenarioId)
            .ifPresent(MiScenarioPriceInterp::softDelete);
    }

    /**
     * [시나리오 그룹 삭제 cascade]
     * 그룹 내 가격전망 해석 전부를 soft delete 한다. (그룹 삭제 트랜잭션에 참여)
     */
    @Transactional
    public void softDeleteByGroup(Long scenarioGroupId) {
        priceInterpRepository.findByScenarioGroupId(scenarioGroupId).forEach(MiScenarioPriceInterp::softDelete);
    }

    /**
     * 시나리오별 최신 완료 수급모델 실행 run_id
     * 그래프의 latestCompletedRunSeq 판정과 동일 기준(케이스·실행 모두 COMPLETED 중 최신)을 재사용한다.
     */
    private Map<Long, String> latestRunByScenario(List<Long> scenarioIds) {
        return scenarioCaseRepository.findLatestCompletedRunIdByScenarioIds(scenarioIds).stream()
            .collect(Collectors.toMap(ScenarioLatestRunRow::scenarioId, ScenarioLatestRunRow::runId));
    }

    /**
     * 시나리오 단위 해석 조회
     */
    private MiScenarioPriceInterp load(Long scenarioGroupId, Long scenarioId) {
        return priceInterpRepository.findActiveByGroupIdAndScenarioId(scenarioGroupId, scenarioId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Price interpretation not found: scenarioGroupId=%d, scenarioId=%d".formatted(scenarioGroupId, scenarioId)));
    }

    private void requireActiveGroup(Long scenarioGroupId) {
        if (!scenarioGroupRepository.existsByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %d".formatted(scenarioGroupId));
        }
    }

    private static Map<Long, MiScenarioPriceInterp> byScenarioId(List<MiScenarioPriceInterp> rows) {
        return rows.stream().collect(Collectors.toMap(row -> row.getScenario().getScenarioId(), Function.identity()));
    }

}
