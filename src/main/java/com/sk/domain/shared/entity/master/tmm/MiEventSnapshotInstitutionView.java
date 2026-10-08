package com.sk.domain.shared.entity.master.tmm;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Immutable;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [MCP Snapshot] Snapshot 저장 당시 활성 기관 전망의 사본 (조회 전용). 원본 tmm_mi_institution_view 의 현재 값으로 대체하지 않는다.
 * crt_dtm/crt_usr_logn_id 는 이력 행 생성 정보이며 전망 발행일은 viewDate 다.
 */
@Getter
@Entity
@Immutable
@Comment("MI Event Snapshot 시점 기관 전망 이력")
@Table(name = "tmm_mi_mievent_snapshot_institution_view_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEventSnapshotInstitutionView extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mi_event_snapshot_institution_view_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("Snapshot 기관 전망 이력 PK")
    private Long miEventSnapshotInstitutionViewId;

    @Column(name = "mi_event_snapshot_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("부모 Snapshot ID")
    private Long miEventSnapshotId;

    @Column(name = "institution_view_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("원본 기관 전망 ID")
    private Long institutionViewId;

    @Column(name = "mi_record_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("전망 근거 Record ID")
    private Long miRecordId;

    @Column(name = "institution_name", length = 200, nullable = false)
    @Comment("기관명")
    private String institutionName;

    @Column(name = "view_date", nullable = false)
    @Comment("전망 발행일")
    private LocalDate viewDate;

    @Column(name = "view_summary", columnDefinition = "longtext", nullable = false)
    @Comment("당시 전망 내용")
    private String viewSummary;

    @Column(name = "source_crt_dtm", nullable = false)
    @Comment("복사 당시 원본 전망 DB 생성 시각")
    private LocalDateTime sourceCreateDt;

    @Column(name = "source_mod_dtm", nullable = false)
    @Comment("복사 당시 원본 전망 DB 수정 시각")
    private LocalDateTime sourceModifyDt;

    /** 테스트 데이터용. 운영 코드에서 저장하지 않는다 (@Immutable). */
    @Builder
    public MiEventSnapshotInstitutionView(Long miEventSnapshotInstitutionViewId, Long miEventSnapshotId, Long institutionViewId,
                                          Long miRecordId, String institutionName, LocalDate viewDate, String viewSummary,
                                          LocalDateTime sourceCreateDt, LocalDateTime sourceModifyDt) {
        this.miEventSnapshotInstitutionViewId = miEventSnapshotInstitutionViewId;
        this.miEventSnapshotId = miEventSnapshotId;
        this.institutionViewId = institutionViewId;
        this.miRecordId = miRecordId;
        this.institutionName = institutionName;
        this.viewDate = viewDate;
        this.viewSummary = viewSummary;
        this.sourceCreateDt = sourceCreateDt;
        this.sourceModifyDt = sourceModifyDt;
    }

}
