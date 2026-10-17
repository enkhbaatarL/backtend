package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * [MCP Event Generate] 이벤트 생성 비동기 작업 상태.
 *
 * 적재·갱신 주체는 B.E 다. 접수 시 RUNNING 으로 만들고, 백그라운드에서 이벤트 생성 서버 응답을 받으면 결과(result_payload)나
 * 실패 사유를 남긴다. 실행 중인 인스턴스는 heartbeat_at 을 주기적으로 갱신하며, 갱신이 끊긴 RUNNING 작업은 INTERRUPTED 로 정리된다.
 * DDL: docs/mcp-event-generation-job-ddl.sql
 */
@Getter
@Entity
@Comment("MCP 이벤트 생성 비동기 작업 상태")
@Table(name = "tmd_mcp_job_mi_event_generation_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEventGenerationJob extends BaseEntity {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_generation_job_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("이벤트 생성 작업 ID")
    private Long eventGenerationJobId;

    @Column(name = "usr_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("요청 사용자번호")
    private Integer userNo;

    @Column(name = "user_query", nullable = false, columnDefinition = "mediumtext")
    @Comment("사용자 입력 문장")
    private String userQuery;

    @Column(name = "date_from", length = 10)
    @Comment("근거 조회 시작일")
    private String dateFrom;

    @Column(name = "date_to", length = 10)
    @Comment("근거 조회 종료일")
    private String dateTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Comment("작업 상태 (RUNNING/SUCCEEDED/FAILED/INTERRUPTED)")
    private EventGenerationJobStatus status;

    @Column(name = "result_payload", columnDefinition = "json")
    @Comment("생성 서버 응답 data 원문")
    private String resultPayload;

    @Column(name = "error_code", length = 20)
    @Comment("실패 코드")
    private String errorCode;

    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    @Comment("실패 메시지")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    @Comment("작업 시작 일시")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    @Comment("작업 종료 일시")
    private LocalDateTime completedAt;

    @Column(name = "heartbeat_at", nullable = false)
    @Comment("마지막 heartbeat 일시")
    private LocalDateTime heartbeatAt;

    private MiEventGenerationJob(Integer userNo, String userQuery, String dateFrom, String dateTo, LocalDateTime now) {
        this.userNo = userNo;
        this.userQuery = userQuery;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.status = EventGenerationJobStatus.RUNNING;
        this.startedAt = now;
        this.heartbeatAt = now;
    }

    public static MiEventGenerationJob start(Integer userNo, String userQuery, String dateFrom, String dateTo, LocalDateTime now) {
        return new MiEventGenerationJob(userNo, userQuery, dateFrom, dateTo, now);
    }

    /** 같은 입력(문장·기간)의 요청인지. 진행 중 작업 중복 접수 판단에 쓴다. */
    public boolean hasSameInput(String userQuery, String dateFrom, String dateTo) {
        return this.userQuery.equals(userQuery) && equalsNullable(this.dateFrom, dateFrom) && equalsNullable(this.dateTo, dateTo);
    }

    /**
     * 성공 종료. 이미 INTERRUPTED 로 정리된 작업이라도 실제 결과가 도착했으면 결과로 덮어쓴다.
     */
    public void succeed(String resultPayload, LocalDateTime now) {
        this.status = EventGenerationJobStatus.SUCCEEDED;
        this.resultPayload = resultPayload;
        this.errorCode = null;
        this.errorMessage = null;
        this.completedAt = now;
    }

    public void fail(String errorCode, String errorMessage, LocalDateTime now) {
        this.status = EventGenerationJobStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = truncate(errorMessage);
        this.completedAt = now;
    }

    private static boolean equalsNullable(String a, String b) {
        return Objects.equals(a, b);
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }

}
