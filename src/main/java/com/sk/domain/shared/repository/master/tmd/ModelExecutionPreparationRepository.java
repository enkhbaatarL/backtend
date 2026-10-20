package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ModelExecutionPreparation;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModelExecutionPreparationRepository extends JpaRepository<ModelExecutionPreparation, Long> {

    Optional<ModelExecutionPreparation> findByCaseIdAndIsDeletedFalse(Long caseId);

    Optional<ModelExecutionPreparation> findFirstByScenarioIdAndAgentProposalIdAndIsDeletedFalseOrderByExecPrepIdDesc(
        Long scenarioId,
        Long agentProposalId
    );

    Optional<ModelExecutionPreparation> findFirstByScenarioIdAndIsDeletedFalseOrderByExecPrepIdDesc(Long scenarioId);

    boolean existsByScenarioIdAndStatusAndExecPrepIdGreaterThanAndIsDeletedFalse(
        Long scenarioId,
        ModelExecutionPreparationStatus status,
        Long execPrepId
    );

    /**
     * 그룹에 적용된 MI Base 후보: 시나리오별로 마지막 RESET 이후 제안이 연결된 실행준비 행 중 최신 1건의 제안 mi_base_case_id (최신 제안 순).
     * 최신 행이 제안 없는 행(생성 중·통신 중단·Agent 거절)이면 직전 제안 행을 보므로 그룹 기준이 사라지지 않고,
     * 시나리오당 1건이라 같은 시나리오의 지난 제안은 섞이지 않는다. mi_base_case_id가 없는 이전 제안은 제외된다.
     */
    @Query("""
        SELECT p.miBaseCaseId
          FROM ModelExecutionPreparation e, AgentModelParamProposal p, MiScenario s
         WHERE s.scenarioGroup.scenarioGroupId = :scenarioGroupId
           AND s.isDeleted = false
           AND e.scenarioId = s.scenarioId
           AND e.execPrepId = (
               SELECT MAX(latest.execPrepId)
                 FROM ModelExecutionPreparation latest
                WHERE latest.scenarioId = e.scenarioId
                  AND latest.isDeleted = false
                  AND latest.agentProposalId IS NOT NULL
                  AND latest.execPrepId > COALESCE((
                      SELECT MAX(reset.execPrepId)
                        FROM ModelExecutionPreparation reset
                       WHERE reset.scenarioId = e.scenarioId
                         AND reset.status = com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus.RESET
                         AND reset.isDeleted = false
                  ), 0)
           )
           AND p.modelParamProposalId = e.agentProposalId
           AND p.isDeleted = false
           AND p.miBaseCaseId IS NOT NULL
         ORDER BY p.modelParamProposalId DESC
        """)
    List<Long> findGroupMiBaseCaseIds(@Param("scenarioGroupId") Long scenarioGroupId);

    @Query("""
        SELECT p
          FROM ModelExecutionPreparation p
         WHERE p.scenarioId = :scenarioId
           AND p.isDeleted = false
           AND p.status IN :statuses
         ORDER BY p.execPrepId DESC
        """)
    Optional<ModelExecutionPreparation> findLatestByScenarioIdAndStatuses(
        @Param("scenarioId") Long scenarioId,
        @Param("statuses") Collection<ModelExecutionPreparationStatus> statuses
    );

    /**
     * 지정 상태의 in-flight 실행준비가 있는 시나리오 ID 집합 조회.
     * activeScenarioId는 in-flight 이고 미삭제인 행에만 값이 있는 unique helper 컬럼이라
     * 삭제 필터가 불필요하고 시나리오당 결과가 최대 1건이다.
     */
    @Query("""
        SELECT p.activeScenarioId
          FROM ModelExecutionPreparation p
         WHERE p.activeScenarioId IN :scenarioIds
           AND p.status IN :statuses
        """)
    List<Long> findActiveScenarioIdsByStatuses(
        @Param("scenarioIds") Collection<Long> scenarioIds,
        @Param("statuses") Collection<ModelExecutionPreparationStatus> statuses
    );

    @Query("""
        SELECT p
          FROM ModelExecutionPreparation p
         WHERE p.scenarioId = :scenarioId
           AND p.isDeleted = false
           AND p.status IN :statuses
         ORDER BY p.execPrepId DESC
        """)
    List<ModelExecutionPreparation> findCallbackCandidates(
        @Param("scenarioId") Long scenarioId,
        @Param("statuses") Collection<ModelExecutionPreparationStatus> statuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ModelExecutionPreparation p
           SET p.agentProposalId = :agentProposalId,
               p.modifyUserId = :actorLoginId,
               p.modifyDt = :modifiedAt
         WHERE p.execPrepId = :execPrepId
           AND p.agentProposalId IS NULL
        """)
    int writeAgentProposalIdIfAbsent(
        @Param("execPrepId") Long execPrepId,
        @Param("agentProposalId") Long agentProposalId,
        @Param("actorLoginId") String actorLoginId,
        @Param("modifiedAt") LocalDateTime modifiedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ModelExecutionPreparation p
           SET p.status = :nextStatus,
               p.errorMessage = :errorMessage,
               p.modifyUserId = :actorLoginId,
               p.modifyDt = :modifiedAt
         WHERE p.execPrepId = :execPrepId
           AND p.status IN :currentStatuses
        """)
    int transitionStatus(
        @Param("execPrepId") Long execPrepId,
        @Param("currentStatuses") Collection<ModelExecutionPreparationStatus> currentStatuses,
        @Param("nextStatus") ModelExecutionPreparationStatus nextStatus,
        @Param("errorMessage") String errorMessage,
        @Param("actorLoginId") String actorLoginId,
        @Param("modifiedAt") LocalDateTime modifiedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ModelExecutionPreparation p
           SET p.caseId = :caseId,
               p.status = :executingStatus,
               p.errorMessage = NULL,
               p.modifyUserId = :actorLoginId,
               p.modifyDt = :modifiedAt
         WHERE p.execPrepId = :execPrepId
           AND p.status = :preparingStatus
           AND p.caseId IS NULL
        """)
    int attachCaseAndMarkExecuting(
        @Param("execPrepId") Long execPrepId,
        @Param("caseId") Long caseId,
        @Param("preparingStatus") ModelExecutionPreparationStatus preparingStatus,
        @Param("executingStatus") ModelExecutionPreparationStatus executingStatus,
        @Param("actorLoginId") String actorLoginId,
        @Param("modifiedAt") LocalDateTime modifiedAt
    );
}
