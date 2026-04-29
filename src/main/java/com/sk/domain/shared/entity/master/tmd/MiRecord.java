package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

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

// note: DB PK는 (mi_record_id, first_created) 복합키지만 mi_record_id 단독으로 unique 하므로 JPA는 단일 PK로 매핑
@Getter
@Entity
@Comment("TMD_MI레코드 (외부 데이터로부터 수집된 MI Record 단위)")
@Table(name = "tmd_mi_record")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mi_record_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("MI Record 일련번호")
    private Integer miRecordId;

    @Column(name = "first_created", nullable = false)
    @Comment("Record 최초생성일시")
    private LocalDateTime firstCreated;

    @Column(name = "record_type", length = 20, nullable = false)
    @Comment("데이터 유형 - NEWS, REPORT, EMAIL")
    private String recordType;

    // note: tmd_nws_smry.nws_smry_id가 Long으로 매핑되어 있어 NEWS 경로 호환을 위해 Long 사용
    @Column(name = "source_obj_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("데이터 소스 ID (record_type별로 다른 원본 테이블의 PK)")
    private Long sourceObjId;

    @Builder
    public MiRecord(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer miRecordId, LocalDateTime firstCreated, String recordType, Long sourceObjId) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.miRecordId = miRecordId;
        this.firstCreated = firstCreated;
        this.recordType = recordType;
        this.sourceObjId = sourceObjId;
    }
}
