package com.sk.domain.shared.entity.master.tmm;

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
 * [MCP Snapshot] Snapshot 버전 생성에 사용된 근거 Record 의 당시 사본 (조회 전용).
 * crt_dtm/crt_usr_logn_id 는 이력 행 생성 정보이며 Record 발생 시각은 recordFirstCreated 다.
 */
@Getter
@Entity
@Immutable
@Comment("MI Event Snapshot 갱신 근거 Record 이력")
@Table(name = "tmm_mi_mievent_snapshot_record_map")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEventSnapshotRecordMap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mi_event_snapshot_record_map_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("Snapshot 근거 Record 이력 PK")
    private Long miEventSnapshotRecordMapId;

    @Column(name = "mi_event_snapshot_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("부모 Snapshot ID")
    private Long miEventSnapshotId;

    @Column(name = "mi_record_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("근거 Record ID")
    private Long miRecordId;

    @Column(name = "record_type", length = 20, nullable = false)
    @Comment("Record 유형 (NEWS/REPORT/EMAIL)")
    private String recordType;

    @Column(name = "ref_nm", length = 50)
    @Comment("당시 Record 출처명")
    private String refNm;

    @Column(name = "record_first_created", nullable = false)
    @Comment("원본 Record 발생 시각 (tmd_mi_record.first_created 복사값)")
    private LocalDateTime recordFirstCreated;

    /** 테스트 데이터용. 운영 코드에서 저장하지 않는다 (@Immutable). */
    @Builder
    public MiEventSnapshotRecordMap(Long miEventSnapshotRecordMapId, Long miEventSnapshotId, Long miRecordId,
                                    String recordType, String refNm, LocalDateTime recordFirstCreated) {
        this.miEventSnapshotRecordMapId = miEventSnapshotRecordMapId;
        this.miEventSnapshotId = miEventSnapshotId;
        this.miRecordId = miRecordId;
        this.recordType = recordType;
        this.refNm = refNm;
        this.recordFirstCreated = recordFirstCreated;
    }

}
