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

    Optional<ModelExecutionPreparation> findByScenarioIdAndAgentProposalIdAndIsDeletedFalse(Long scenarioId, Long agentProposalId);

    Optional<ModelExecutionPreparation> findFirstByScenarioIdAndIsDeletedFalseOrderByExecPrepIdDesc(Long scenarioId);

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
}
