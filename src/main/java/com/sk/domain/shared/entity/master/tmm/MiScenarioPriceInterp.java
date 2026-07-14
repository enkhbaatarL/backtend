package com.sk.domain.shared.entity.master.tmm;

import com.sk.domain.shared.converter.PriceInterpretationStatusConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.nextgen.PriceInterpretationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 시나리오별 가격전망 해석
 *
 * 시나리오 1 : 활성 가격전망해석 1 (논리 규칙). 이 규칙은 DB의 함수 기반 유니크 인덱스
 * (scenario_id, case when del_yn = 'N' then 'N' end)가 강제한다 — 활성 row(del_yn=N)는 키 값이 (scenario_id, 'N')이라 시나리오당 1개만 허용되고, 삭제 row(del_yn=Y)는 키 값이 NULL이라
 * 유니크 검사 대상에서 빠져 무제한 공존한다. 수급모델 재실행에 따른 soft delete가 반복돼도 충돌하지 않는다.
 * 함수 기반 인덱스는 JPA 어노테이션으로 표현할 수 없어 @Table에 유니크를 선언하지 않는다 (DDL이 원본).
 * 물리적으로는 시나리오당 row가 복수일 수 있어 매핑은 ManyToOne이다 (MiSignpost와 동일 패턴).
 *
 * Agent 생성 원본(generatedContent)과 사용자 수정본(currentContent)을 함께 보관한다.
 * 초기화는 Agent 재호출 없이 currentContent = generatedContent로 복원한다.
 *
 * 생성은 시나리오별 증분 + 재실행이다. 수급모델이 N회차 재실행되면(= sourceRunId가 최신 완료 실행과
 * 다르면) 재실행 대상이 되며, 재실행 시 원본/수정본을 새 결과로 교체한다. 재실행 전까지 generatedContent는
 * 초기화 복원의 기준선이다. 생성 대상 판정은 needsRegeneration 한 곳에 모은다.
 *
 * 콜백이 유실되어 GENERATING에 갇히는 상황은 만료(GENERATING_TIMEOUT 초과) 판정으로 복구한다 —
 * 만료된 생성중 row는 다시 생성 대상이 된다.
 */
@Getter
@Entity
@Comment("시나리오별 가격전망 해석")
@Table(name = "tmm_mi_scenario_prc_interp_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiScenarioPriceInterp extends BaseEntity {

    /**
     * 생성중(GENERATING) 만료 시간.
     * Agent가 접수 후 콜백이 없으면 생성중으로 갇히므로 mod_dtm 기준 이 시간이 지나면 실패로 간주해 재생성을 허용한다.
     */
    public static final Duration GENERATING_TIMEOUT = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prc_interp_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("가격전망해석ID")
    private Long priceInterpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    @Comment("시나리오ID")
    private MiScenario scenario;

    @Column(name = "org_cntt", columnDefinition = "text")
    @Comment("원본내용 (Agent 생성 원본, 초기화 복원·재실행 기준)")
    private String generatedContent;

    @Column(name = "cur_cntt", columnDefinition = "text")
    @Comment("현재내용 (사용자 수정본, 표시용)")
    private String currentContent;

    @Convert(converter = PriceInterpretationStatusConverter.class)
    @Column(name = "status", nullable = false, length = 50)
    @ColumnDefault("'generating'")
    @Comment("상태 (generating:생성중 / generated:생성완료 / failed:생성실패)")
    private PriceInterpretationStatus status;

    @Column(name = "fail_reason", columnDefinition = "text")
    @Comment("실패사유 (Agent 생성 실패 사유)")
    private String failReason;

    @Column(name = "src_run_id", length = 20)
    @Comment("소스실행ID (해석 근거 수급모델 실행ID, tmd_mdl_scnrcaseh.run_id 논리참조·물리 FK 없음. 최신 완료 run_id와 다르면 재실행 대상)")
    private String sourceRunId;

    @Column(name = "rvsn_no", nullable = false, columnDefinition = "int unsigned")
    @ColumnDefault("0")
    @Comment("리비전번호 (수정/재실행 시 증가, 참고용, 충돌검증 미사용)")
    private Integer revision;

    @Builder
    private MiScenarioPriceInterp(MiScenario scenario, PriceInterpretationStatus status, Integer revision) {
        this.scenario = scenario;
        this.status = status;
        this.revision = revision;
    }

    /**
     * POST 생성 시점의 초기 row를 만든다. content 없이 GENERATING 상태로 선생성한다.
     * sourceRunId는 착수 시점의 최신 완료 실행 ID로 별도 스탬프한다.
     */
    public static MiScenarioPriceInterp toEntity(MiScenario scenario) {
        return MiScenarioPriceInterp.builder()
            .scenario(scenario)
            .status(PriceInterpretationStatus.GENERATING)
            .revision(0)
            .build();
    }

    /**
     * 화면/판정용 유효 상태의 판정
     * 만료된 생성중(GENERATING -> GENERATING_TIMEOUT 초과)은 실패로 간주
     */
    public PriceInterpretationStatus displayStatus() {
        if (status == null) return PriceInterpretationStatus.FAILED;
        if (isGenerationExpired()) return PriceInterpretationStatus.FAILED;
        return status;
    }

    /**
     * 생성 대상 여부 판정
     * 대상
     *  1. 실패(재시도, 만료된 생성중 포함)
     *  2. 가격전망해석은 있으나 수급모델 실행이 최신 완료 run과 다름(재실행 stale)
     */
    public boolean needsRegeneration(String latestRunId) {
        return switch (displayStatus()) {
            case FAILED -> true;
            case GENERATING -> false;
            case GENERATED -> !latestRunId.equals(sourceRunId);
        };
    }

    /**
     * 유효한(만료되지 않은) 생성 진행 중 여부. 수정/초기화 차단 등 사용자 게이트에 사용한다.
     * 만료된 생성중은 진행 중으로 보지 않는다 — 사용자가 기존 content를 다시 만질 수 있어야 하므로.
     */
    public boolean isGenerationInProgress() {
        return displayStatus() == PriceInterpretationStatus.GENERATING;
    }

    /**
     * 생성중 만료 여부
     * Agent 콜백이 유실된 채 GENERATING_TIMEOUT을 초과하면 만료로 본다. (mod_dtm 기준)
     */
    private boolean isGenerationExpired() {
        return status == PriceInterpretationStatus.GENERATING
            && getModifyDt() != null
            && getModifyDt().isBefore(LocalDateTime.now().minus(GENERATING_TIMEOUT));
    }

    /**
     * 기존 해석을 재생성 대상으로 되돌린다. 실패 사유를 지우고 GENERATING으로 전환한다.
     * 실패 재시도 / 만료 복구 / 재실행(stale) 재생성에 공용이며, 콜백 도착 전까지 기존 content는 그대로 유지된다.
     */
    public void markGenerating() {
        this.failReason = null;
        this.status = PriceInterpretationStatus.GENERATING;
        // 만료된 GENERATING 재시도는 status/failReason/sourceRunId가 전부 기존 값과 같아 dirty가 없을 수 있다.
        // UPDATE가 생략되면 mod_dtm(만료 판정 기준)이 갱신되지 않아 즉시 다시 '만료'가 되므로 명시적으로 touch한다.
        this.modifyDt = LocalDateTime.now();
    }

    /**
     * 해석이 근거한 수급모델 실행 ID를 스탬프한다. (재실행 stale 판정 기준)
     */
    public void updateRunId(String runId) {
        this.sourceRunId = runId;
    }

    /**
     * Agent 콜백 성공 시 원본을 적재
     * GENERATED로 업데이트
     */
    public void applyGenerated(String content) {
        this.generatedContent = content;
        this.currentContent = content;
        this.failReason = null;
        this.status = PriceInterpretationStatus.GENERATED;
    }

    /**
     * Agent 콜백 실패 시 사유를 적재하고 FAILED로 업데이트
     */
    public void applyFailed(String failReason) {
        this.failReason = failReason;
        this.status = PriceInterpretationStatus.FAILED;
    }

    /**
     * 사용자 수정본을 반영한다.
     * 현재 상태와 무관하게 덮어쓰며 revision을 증가시킨다.
     */
    public void edit(String content) {
        this.currentContent = content;
        this.revision = this.revision + 1;
    }

    /**
     * Agent 생성 원본으로 복원한다(초기화). Agent를 재호출하지 않는다.
     */
    public void reset() {
        this.currentContent = this.generatedContent;
        this.revision = this.revision + 1;
    }

    /**
     * DB 상태 기준 생성 중 여부
     */
    public boolean isGenerating() {
        return this.status != null && this.status.isGenerating();
    }

    /**
     * 초기화 가능 여부. 유효한 생성 진행 중(만료 제외)이 아니고 성공한 Agent 원본이 있어야 한다.
     */
    public boolean isResettable() {
        return !isGenerationInProgress() && this.generatedContent != null;
    }

}
