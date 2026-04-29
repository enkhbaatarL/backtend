package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

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

// note: 본 테이블은 BaseEntity 미사용 (DDL상 crt_dtm만 존재, 감사 컬럼 / del_yn 없음)
@Getter
@Entity
@Comment("TMD_MI이벤트 소스 매핑 (Event ↔ MI Record N:M)")
@Table(name = "tmd_mievent_source_mapm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEventSourceMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("매핑 일련번호")
    private Integer mapId;

    @Column(name = "mi_event_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("MI Event ID FK")
    private Long miEventId;

    @Column(name = "mi_record_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("MI Record ID FK")
    private Integer miRecordId;

    @Column(name = "crt_dtm", nullable = false)
    @Comment("생성일시")
    private LocalDateTime createDt;

    @Builder
    public MiEventSourceMap(Long miEventId, Integer miRecordId, LocalDateTime createDt) {
        this.miEventId = miEventId;
        this.miRecordId = miRecordId;
        this.createDt = createDt;
    }
}
