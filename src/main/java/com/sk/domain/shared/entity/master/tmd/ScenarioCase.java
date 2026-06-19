package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Comment("TMD_시나리오케이스마스터")
@Table(
    name = "tmd_mdl_scnrcasem",
    indexes = {
        @Index(name = "tmd_mdl_scnrcasem_ix01", columnList = "del_yn, crt_dtm"),
        @Index(name = "tmd_mdl_scnrcasem_ix02", columnList = "scnr_id, case_stat_cd, case_id"),
        @Index(name = "tmd_mdl_scnrcasem_ix03", columnList = "last_run_id, del_yn")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_mdl_scnrcasem_uq01", columnNames = {"actv_scnr_id"})
    }
)
@Check(
    name = "tmd_mdl_scnrcasem_ck01",
    constraints = "(exec_type_cd = 'SCENARIO') = (scnr_id is not null)"
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

    @Column(name = "scnr_id", columnDefinition = "int unsigned")
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Comment("시나리오ID (MI 수급모델 실행 연계, MI Fundamental Model은 null, tmm_mi_scenario_m.scenario_id 논리참조, 물리 FK 없음)")
    private Long scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exec_type_cd", nullable = false, length = 20)
    @ColumnDefault("'INDEPENDENT'")
    @Comment("수급모델실행유형코드 (INDEPENDENT/SCENARIO/MI_BASE)")
    private FundamentalModelExecType execType;

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

    @Column(
        name = "actv_scnr_id",
        columnDefinition = "int unsigned generated always as (if(case_stat_cd = 'RUNNING', scnr_id, null)) stored",
        insertable = false,
        updatable = false
    )
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Comment("활성시나리오ID (case_stat_cd = RUNNING 이면 scnr_id, 그 외 NULL. DB STORED generated column, scenario 당 active 1건 unique helper)")
    private Long activeScenarioId;

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
        Long scenarioId,
        FundamentalModelExecType execType,
        String title,
        String description,
        FundamentalModelCaseStatus status,
        String lastRunId
    ) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.caseId = caseId;
        this.userNo = userNo;
        this.scenarioId = scenarioId;
        this.execType = execType;
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
            .execType(FundamentalModelExecType.INDEPENDENT)
            .title(title)
            .description(description)
            .status(FundamentalModelCaseStatus.PENDING)
            .build();
    }

    public static ScenarioCase createScenarioRunning(
        Integer userNo,
        Long scenarioId,
        String title,
        String description,
        String actorLoginId
    ) {
        LocalDateTime now = LocalDateTime.now();
        return ScenarioCase.builder()
            .createDt(now)
            .modifyDt(now)
            .createUserId(actorLoginId)
            .modifyUserId(actorLoginId)
            .isDeleted(Boolean.FALSE)
            .userNo(userNo)
            .scenarioId(scenarioId)
            .execType(FundamentalModelExecType.SCENARIO)
            .title(title)
            .description(description)
            .status(FundamentalModelCaseStatus.RUNNING)
            .build();
    }

    public void attachLatestRun(String runId, String actorLoginId) {
        this.lastRunId = runId;
        this.modifyUserId = actorLoginId;
        this.modifyDt = LocalDateTime.now();
    }

    public void updateMetadata(String title, String description, String actorLoginId) {
        this.title = title;
        this.description = description;
        this.modifyUserId = actorLoginId;
        this.modifyDt = LocalDateTime.now();
    }

    public void softDelete(String actorLoginId) {
        super.softDelete();
        this.modifyUserId = actorLoginId;
        this.modifyDt = LocalDateTime.now();
    }
}
