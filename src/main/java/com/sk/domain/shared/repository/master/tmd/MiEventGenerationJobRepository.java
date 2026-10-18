package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MiEventGenerationJob;
import com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MiEventGenerationJobRepository extends JpaRepository<MiEventGenerationJob, Long> {

    /** 사용자의 진행 중 작업 — 같은 입력 중복 접수 판단. */
    List<MiEventGenerationJob> findByUserNoAndStatusAndIsDeletedFalse(Integer userNo, EventGenerationJobStatus status);

    /** 사용자 본인 작업 1건. 다른 사용자 작업은 없는 것으로 본다. */
    Optional<MiEventGenerationJob> findByEventGenerationJobIdAndUserNoAndIsDeletedFalse(Long eventGenerationJobId, Integer userNo);

    /** 사용자 작업 목록 (최신순). */
    Page<MiEventGenerationJob> findByUserNoAndIsDeletedFalseOrderByEventGenerationJobIdDesc(Integer userNo, Pageable pageable);

    /** 이 인스턴스가 실행 중인 작업의 heartbeat 갱신. */
    @Transactional
    @Modifying
    @Query("""
        update MiEventGenerationJob j set j.heartbeatAt = :now
        where j.eventGenerationJobId in :ids and j.status = com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus.RUNNING""")
    int touchHeartbeat(@Param("ids") Collection<Long> ids, @Param("now") LocalDateTime now);

    /** 종료하는 인스턴스가 맡았던 진행 중 작업을 바로 INTERRUPTED 로 정리. */
    @Transactional
    @Modifying
    @Query("""
        update MiEventGenerationJob j
        set j.status = com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus.INTERRUPTED,
            j.completedAt = :now, j.errorCode = :errorCode, j.errorMessage = :errorMessage
        where j.eventGenerationJobId in :ids and j.status = com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus.RUNNING""")
    int interruptRunning(@Param("ids") Collection<Long> ids, @Param("now") LocalDateTime now,
                         @Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);

    /** heartbeat 가 끊긴 진행 중 작업을 INTERRUPTED 로 정리 (실행하던 인스턴스가 재기동·종료된 경우). */
    @Transactional
    @Modifying
    @Query("""
        update MiEventGenerationJob j
        set j.status = com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus.INTERRUPTED,
            j.completedAt = :now, j.errorCode = :errorCode, j.errorMessage = :errorMessage
        where j.status = com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus.RUNNING and j.heartbeatAt < :staleBefore""")
    int interruptStale(@Param("staleBefore") LocalDateTime staleBefore, @Param("now") LocalDateTime now,
                       @Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);

}
