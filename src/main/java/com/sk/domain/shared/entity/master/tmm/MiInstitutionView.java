package com.sk.domain.shared.entity.master.tmm;

import java.time.LocalDate;

import org.hibernate.annotations.Comment;

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

@Getter
@Entity
@Comment("MI Event 기관전망 상세 (이벤트/원본레코드 연결)")
@Table(name = "tmm_mi_institution_view")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiInstitutionView extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "institution_view_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("기관전망 PK")
    private Long institutionViewId;

    @Column(name = "mi_event_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("MI Event ID")
    private Long miEventId;

    @Column(name = "mi_record_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("추출 원본 MI Record ID")
    private Long miRecordId;

    @Column(name = "institution_name", length = 200, nullable = false)
    @Comment("기관명")
    private String institutionName;

    @Column(name = "view_date", nullable = false)
    @Comment("기관전망일자")
    private LocalDate viewDate;

    @Column(name = "view_summary", columnDefinition = "longtext", nullable = false)
    @Comment("기관전망 요약")
    private String viewSummary;

    @Builder
    public MiInstitutionView(Long institutionViewId, Long miEventId, Long miRecordId,
                             String institutionName, LocalDate viewDate, String viewSummary) {
        this.institutionViewId = institutionViewId;
        this.miEventId = miEventId;
        this.miRecordId = miRecordId;
        this.institutionName = institutionName;
        this.viewDate = viewDate;
        this.viewSummary = viewSummary;
    }

}
