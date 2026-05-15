package com.sk.domain.shared.entity.master.tmm;

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
@Comment("MI 이벤트 유형 L1 마스터")
@Table(name = "tmm_mi_event_type_l1_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEventTypeL1 extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_type_l1_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("이벤트 유형 L1 PK")
    private Long eventTypeL1Id;

    @Column(name = "level_1_code", length = 100, nullable = false, unique = true)
    @Comment("이벤트 유형 L1 코드")
    private String level1Code;

    @Column(name = "level_1_label", length = 200, nullable = false)
    @Comment("이벤트 유형 L1 한글명")
    private String level1Label;

    @Builder
    public MiEventTypeL1(Long eventTypeL1Id, String level1Code, String level1Label) {
        this.eventTypeL1Id = eventTypeL1Id;
        this.level1Code = level1Code;
        this.level1Label = level1Label;
    }

}
