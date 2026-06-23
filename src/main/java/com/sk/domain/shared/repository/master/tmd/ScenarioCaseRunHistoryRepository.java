package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ScenarioCaseRunHistory;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioCaseRunHistoryRepository extends JpaRepository<ScenarioCaseRunHistory, String> {

    @Query(value = """
        SELECT h.run_id
          FROM tmd_mdl_scnrcaseh h
          JOIN tmd_mdl_scnrcasem c
            ON c.case_id = h.case_id
         WHERE h.exec_stat_cd = 'RUNNING'
           AND h.exec_bgn_dtm < NOW() - INTERVAL 10 MINUTE
           AND c.exec_type_cd IN ('SCENARIO', 'INDEPENDENT')
         ORDER BY h.exec_bgn_dtm ASC, h.case_id ASC
         LIMIT 100
        """, nativeQuery = true)
    List<String> findTimedOutRunningRunIds();

    Optional<ScenarioCaseRunHistory> findByRunIdAndIsDeletedFalse(String runId);

    @Query("""
        SELECT h.executionBeginDt
          FROM ScenarioCaseRunHistory h
         WHERE h.runId = :runId
        """)
    Optional<LocalDateTime> findExecutionBeginDtByRunId(@Param("runId") String runId);

    @Query("""
        SELECT CURRENT_TIMESTAMP
          FROM ScenarioCaseRunHistory h
         WHERE h.runId = :runId
        """)
    Optional<LocalDateTime> findCurrentDatabaseTimeByRunId(@Param("runId") String runId);

    @Query("""
        SELECT h.runSeq
          FROM ScenarioCaseRunHistory h
         WHERE h.runId = :runId
        """)
    Optional<Long> findRunSeqByRunId(@Param("runId") String runId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ScenarioCaseRunHistory h
           SET h.status = :terminalStatus,
               h.executionEndDt = :finishedAt,
               h.failureReasonCode = :failureReasonCode,
               h.errorMessage = :errorMessage,
               h.modifyDt = :modifiedAt,
               h.modifyUserId = :actorLoginId
         WHERE h.runId = :runId
           AND h.status = :runningStatus
        """)
    int updateRunningToTerminal(
        @Param("runId") String runId,
        @Param("terminalStatus") FundamentalModelRunStatus terminalStatus,
        @Param("runningStatus") FundamentalModelRunStatus runningStatus,
        @Param("finishedAt") LocalDateTime finishedAt,
        @Param("failureReasonCode") String failureReasonCode,
        @Param("errorMessage") String errorMessage,
        @Param("modifiedAt") LocalDateTime modifiedAt,
        @Param("actorLoginId") String actorLoginId
    );
}
