package com.sk.domain.shared.entity.master.tmm;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.MiRecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("MI Event - MI Record 매핑")
@Table(name = "tmm_mi_mievent_record_map")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEventRecordMap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mi_event_record_map_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("Event-Record 매핑 PK")
    private Long miEventRecordMapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mi_event_id", nullable = false)
    @Comment("MI Event ID FK")
    private MiEvent miEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mi_record_id", nullable = false)
    @Comment("MI Record ID FK")
    private MiRecord miRecord;

    @Builder
    public MiEventRecordMap(MiEvent miEvent, MiRecord miRecord) {
        this.miEvent = miEvent;
        this.miRecord = miRecord;
    }
}
