package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("파라미터제안기반수급모델실행준비이력")
@Table(name = "tmd_mi_model_execpreph")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModelExecutionPreparation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exec_prep_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("파라미터제안기반실행준비ID")
    private Long execPrepId;

    @Column(name = "scnr_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오ID")
    private Long scenarioId;

    @Column(name = "req_usr_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("요청사용자번호")
    private Integer requestUserNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "exec_prep_stat_cd", nullable = false, length = 30)
    @Comment("파라미터제안기반실행준비상태코드")
    private ModelExecutionPreparationStatus status;

    @Column(name = "agnt_prpsl_id", columnDefinition = "int unsigned")
    @Comment("에이전트제안ID")
    private Long agentProposalId;

    @Column(name = "case_id", columnDefinition = "int unsigned")
    @Comment("시나리오케이스ID")
    private Long caseId;

    @Column(name = "actv_scnr_id", insertable = false, updatable = false, columnDefinition = "int unsigned")
    @Comment("활성시나리오ID")
    private Long activeScenarioId;

    @Column(name = "err_msg_cntt", length = 1000)
    @Comment("오류메시지내용")
    private String errorMessage;

    public static ModelExecutionPreparation createRequesting(Long scenarioId, Integer requestUserNo) {
        ModelExecutionPreparation preparation = new ModelExecutionPreparation();
        preparation.scenarioId = scenarioId;
        preparation.requestUserNo = requestUserNo;
        preparation.status = ModelExecutionPreparationStatus.REQUESTING;
        return preparation;
    }

    public static ModelExecutionPreparation createConfirmingExecution(
        Long scenarioId,
        Integer requestUserNo,
        Long agentProposalId
    ) {
        ModelExecutionPreparation preparation = new ModelExecutionPreparation();
        preparation.scenarioId = scenarioId;
        preparation.requestUserNo = requestUserNo;
        preparation.agentProposalId = agentProposalId;
        preparation.status = ModelExecutionPreparationStatus.CONFIRMING;
        return preparation;
    }
}
