package com.sk.domain.shared.entity.master.tmm;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

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
@Comment("MI 이벤트 유형 L2 마스터")
@Table(name = "tmm_mi_event_type_l2_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEventTypeL2 extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_type_l2_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("이벤트 유형 L2 PK")
    private Long eventTypeL2Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_l1_id", nullable = false)
    @Comment("이벤트 유형 L1 FK")
    private MiEventTypeL1 eventTypeL1;

    @Column(name = "level_2_code", length = 150, nullable = false)
    @Comment("이벤트 유형 L2 코드")
    private String level2Code;

    @Column(name = "level_2_label", length = 200, nullable = false)
    @Comment("이벤트 유형 L2 한글명")
    private String level2Label;

    @Builder
    public MiEventTypeL2(Long eventTypeL2Id, MiEventTypeL1 eventTypeL1, String level2Code, String level2Label) {
        this.eventTypeL2Id = eventTypeL2Id;
        this.eventTypeL1 = eventTypeL1;
        this.level2Code = level2Code;
        this.level2Label = level2Label;
    }
}
