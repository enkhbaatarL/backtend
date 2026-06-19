package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ScenarioCaseRunHistory;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioCaseRunHistoryRepository extends JpaRepository<ScenarioCaseRunHistory, String> {

    Optional<ScenarioCaseRunHistory> findByRunIdAndIsDeletedFalse(String runId);

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
