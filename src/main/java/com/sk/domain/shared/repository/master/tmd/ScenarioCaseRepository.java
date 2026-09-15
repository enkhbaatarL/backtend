package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.repository.master.tmd.custom.CustomScenarioCaseRepository;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioCaseRepository extends JpaRepository<ScenarioCase, Long>, CustomScenarioCaseRepository {

    Optional<ScenarioCase> findByCaseIdAndIsDeletedFalse(Long caseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
          FROM ScenarioCase c
         WHERE c.activeBaseYn = 'Y'
        """)
    Optional<ScenarioCase> findCurrentMiBaseForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
          FROM ScenarioCase c
         WHERE c.caseId = :caseId
           AND c.isDeleted = false
        """)
    Optional<ScenarioCase> findActiveCaseForUpdate(@Param("caseId") Long caseId);

    Optional<ScenarioCase> findByCaseIdAndScenarioIdAndExecTypeAndIsDeletedFalse(
        Long caseId,
        Long scenarioId,
        FundamentalModelExecType execType
    );

    Optional<ScenarioCase> findFirstByScenarioIdAndExecTypeAndIsDeletedFalseOrderByCaseIdDesc(
        Long scenarioId,
        FundamentalModelExecType execType
    );

    boolean existsByScenarioIdAndExecTypeAndStatusAndIsDeletedFalse(
        Long scenarioId,
        FundamentalModelExecType execType,
        FundamentalModelCaseStatus status
    );

    @Query("""
        SELECT c.caseId
          FROM ScenarioCase c
         WHERE c.scenarioId IN :scenarioIds
        """)
    List<Long> findCaseIdsByScenarioIds(@Param("scenarioIds") Collection<Long> scenarioIds);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE ScenarioCase c
           SET c.status = :failedStatus,
               c.modifyDt = :modifiedAt,
               c.modifyUserId = :actorLoginId
         WHERE c.scenarioId IN :scenarioIds
           AND c.status = :runningStatus
        """)
    int cancelRunningByScenarioIds(
        @Param("scenarioIds") Collection<Long> scenarioIds,
        @Param("failedStatus") FundamentalModelCaseStatus failedStatus,
        @Param("runningStatus") FundamentalModelCaseStatus runningStatus,
        @Param("modifiedAt") LocalDateTime modifiedAt,
        @Param("actorLoginId") String actorLoginId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE ScenarioCase c
           SET c.isDeleted = true,
               c.modifyDt = :modifiedAt,
               c.modifyUserId = :actorLoginId
         WHERE c.scenarioId IN :scenarioIds
           AND c.isDeleted = false
        """)
    int softDeleteByScenarioIds(
        @Param("scenarioIds") Collection<Long> scenarioIds,
        @Param("modifiedAt") LocalDateTime modifiedAt,
        @Param("actorLoginId") String actorLoginId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ScenarioCase c
           SET c.status = :runningStatus,
               c.lastRunId = :runId,
               c.modifyDt = :modifiedAt,
               c.modifyUserId = :actorLoginId
         WHERE c.caseId = :caseId
           AND c.isDeleted = false
           AND c.execType = :execType
           AND c.status IN :claimableStatuses
        """)
    int claimRun(
        @Param("caseId") Long caseId,
        @Param("runId") String runId,
        @Param("execType") FundamentalModelExecType execType,
        @Param("runningStatus") FundamentalModelCaseStatus runningStatus,
        @Param("claimableStatuses") Collection<FundamentalModelCaseStatus> claimableStatuses,
        @Param("modifiedAt") LocalDateTime modifiedAt,
        @Param("actorLoginId") String actorLoginId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ScenarioCase c
           SET c.status = :status,
               c.modifyDt = :modifiedAt,
               c.modifyUserId = :actorLoginId
         WHERE c.lastRunId = :runId
           AND c.isDeleted = false
        """)
    int updateLatestRunStatus(
        @Param("runId") String runId,
        @Param("status") FundamentalModelCaseStatus status,
        @Param("modifiedAt") LocalDateTime modifiedAt,
        @Param("actorLoginId") String actorLoginId
    );
}
