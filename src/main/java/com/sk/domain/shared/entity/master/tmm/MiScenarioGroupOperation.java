package com.sk.domain.shared.entity.master.tmm;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Immutable;

/**
 * 시나리오 그룹 비동기 작업 상태 (Agent v4 read-only).
 *
 * 적재·갱신 주체는 Agent이며 B.E는 조회 전용이다. 생성 진행의 원본 상태로,
 * 사용자별 진행중 작업 복구(브라우저 재시작 후 재연결)의 근거가 된다.
 * status 어휘(queued/running/succeeded/failed/interrupted)는 Agent 소유라 enum 대신 문자열로 통과시킨다.
 * retry_summary, presentation_snapshot, last_error 등 무거운 JSON 컬럼은 B.E가 읽지 않아 매핑하지 않는다.
 */
@Getter
@Entity
@Immutable
@Comment("시나리오 그룹 비동기 작업 상태 Agent read-only")
@Table(name = "tmm_mi_scenario_group_operation_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiScenarioGroupOperation extends BaseEntity {

    @Id
    @Column(name = "operation_id", nullable = false, length = 26)
    @Comment("작업 ULID")
    private String operationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_group_id", nullable = false)
    @Comment("시나리오 그룹 ID")
    private MiScenarioGroup scenarioGroup;

    @Column(name = "operation_type", nullable = false, length = 30)
    @Comment("작업 유형 (create/validation/signpost/confirmation)")
    private String operationType;

    @Column(name = "engine_version", nullable = false, length = 20)
    @Comment("실행 엔진 버전")
    private String engineVersion;

    @Column(name = "status", nullable = false, length = 20)
    @Comment("작업 상태 (queued/running/succeeded/failed/interrupted)")
    private String status;

    @Column(name = "current_phase", length = 50)
    @Comment("현재 처리 단계")
    private String currentPhase;

    @Column(name = "snapshot_revision", nullable = false)
    @Comment("표시 스냅샷 revision")
    private Integer snapshotRevision;

    @Column(name = "latest_sequence", nullable = false)
    @Comment("최신 이벤트 sequence")
    private Integer latestSequence;

    @Column(name = "last_error", columnDefinition = "json")
    @Comment("최종 오류 정보 (실패 사유 표시용)")
    private String lastError;

    @Column(name = "started_at")
    @Comment("작업 시작 일시")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    @Comment("작업 완료 일시")
    private LocalDateTime completedAt;

    @Column(name = "heartbeat_at")
    @Comment("마지막 heartbeat 일시")
    private LocalDateTime heartbeatAt;

}
