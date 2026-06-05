package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Comment("TMD_시나리오케이스마스터")
@Table(
    name = "tmd_scncasem",
    indexes = {
        @Index(name = "tmd_scncasem_ix01", columnList = "del_yn, crt_dtm")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오케이스ID")
    private Long caseId;

    @Column(name = "usr_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자번호")
    private Integer userNo;

    @Column(name = "case_titl_nm", nullable = false, length = 200)
    @Comment("케이스제목명")
    private String title;

    @Column(name = "case_expn", columnDefinition = "TEXT")
    @Comment("케이스설명")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_stat_cd", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    @Comment("케이스상태코드")
    private FundamentalModelCaseStatus status;

    @Column(name = "last_run_id", length = 20)
    @Comment("최신실행ID")
    private String lastRunId;

    @Builder
    public ScenarioCase(
        LocalDateTime createDt,
        LocalDateTime modifyDt,
        String createUserId,
        String modifyUserId,
        Boolean isDeleted,
        Long caseId,
        Integer userNo,
        String title,
        String description,
        FundamentalModelCaseStatus status,
        String lastRunId
    ) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.caseId = caseId;
        this.userNo = userNo;
        this.title = title;
        this.description = description;
        this.status = status;
        this.lastRunId = lastRunId;
    }

    public static ScenarioCase createPending(Integer userNo, String title, String description, String actorLoginId) {
        LocalDateTime now = LocalDateTime.now();
        return ScenarioCase.builder()
            .createDt(now)
            .modifyDt(now)
            .createUserId(actorLoginId)
            .modifyUserId(actorLoginId)
            .isDeleted(Boolean.FALSE)
            .userNo(userNo)
            .title(title)
            .description(description)
            .status(FundamentalModelCaseStatus.PENDING)
            .build();
    }
}
