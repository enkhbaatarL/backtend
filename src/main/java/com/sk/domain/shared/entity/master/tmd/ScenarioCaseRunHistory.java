package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_시나리오케이스이력")
@Table(
    name = "tmd_scncaseh",
    indexes = {
        @Index(name = "tmd_scncaseh_ix01", columnList = "exec_stat_cd, exec_bgn_dtm")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioCaseRunHistory extends BaseEntity {

    @Id
    @Column(name = "run_id", nullable = false, length = 20)
    @Comment("실행ID")
    private String runId;

    @Column(name = "run_seq", columnDefinition = "bigint unsigned", unique = true)
    @Comment("실행순번")
    private Long runSeq;

    @Column(name = "case_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오케이스ID")
    private Long caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exec_stat_cd", nullable = false, length = 20)
    @ColumnDefault("'RUNNING'")
    @Comment("실행상태코드")
    private FundamentalModelRunStatus status;

    @Column(name = "fail_rsn_cd", length = 20)
    @Comment("실패사유코드")
    private String failureReasonCode;

    @Column(name = "err_msg_cntt", length = 2000)
    @Comment("오류메시지내용")
    private String errorMessage;

    @Column(name = "exec_bgn_dtm")
    @Comment("실행시작일시")
    private LocalDateTime executionBeginDt;

    @Column(name = "exec_end_dtm")
    @Comment("실행종료일시")
    private LocalDateTime executionEndDt;

    @Builder
    public ScenarioCaseRunHistory(
        LocalDateTime createDt,
        LocalDateTime modifyDt,
        String createUserId,
        String modifyUserId,
        Boolean isDeleted,
        String runId,
        Long runSeq,
        Long caseId,
        FundamentalModelRunStatus status,
        String failureReasonCode,
        String errorMessage,
        LocalDateTime executionBeginDt,
        LocalDateTime executionEndDt
    ) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.runId = runId;
        this.runSeq = runSeq;
        this.caseId = caseId;
        this.status = status;
        this.failureReasonCode = failureReasonCode;
        this.errorMessage = errorMessage;
        this.executionBeginDt = executionBeginDt;
        this.executionEndDt = executionEndDt;
    }
}
