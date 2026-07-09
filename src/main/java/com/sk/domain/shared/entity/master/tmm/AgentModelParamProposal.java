package com.sk.domain.shared.entity.master.tmm;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.nextgen.AgentModelParamProposalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Immutable;

@Getter
@Entity
@Immutable
@Comment("수급모델 파라미터 제안 Agent read-only")
@Table(name = "tmm_mi_model_param_proposal_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentModelParamProposal extends BaseEntity {

    @Id
    @Column(name = "model_param_proposal_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("수급모델 파라미터 제안 ID")
    private Long modelParamProposalId;

    @Column(name = "scenario_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오 ID")
    private Long scenarioId;

    @Column(name = "scenario_group_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오 그룹 ID")
    private Long scenarioGroupId;

    @Column(name = "mi_event_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("MI 이벤트 ID")
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Comment("상태")
    private AgentModelParamProposalStatus status;

    @Column(name = "proposal_parameters", columnDefinition = "json")
    @Comment("제안 파라미터 배열 JSON")
    private String proposalParameters;

    @Column(name = "confirmation_parameters", columnDefinition = "json")
    @Comment("확정 파라미터 배열 JSON")
    private String confirmationParameters;

    @Column(name = "proposal_completed_dtm")
    @Comment("제안 완료일시")
    private LocalDateTime proposalCompletedAt;

    @Column(name = "confirmed_dtm")
    @Comment("확정일시")
    private LocalDateTime confirmedAt;

    @Column(name = "error_code", length = 100)
    @Comment("오류 코드")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    @Comment("오류 메시지")
    private String errorMessage;

    @Column(name = "callback_status", nullable = false, length = 50)
    @Comment("Callback 상태")
    private String callbackStatus;

    @Column(name = "callback_sent_dtm")
    @Comment("Callback 성공일시")
    private LocalDateTime callbackSentAt;
}
