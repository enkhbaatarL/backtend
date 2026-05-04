package com.sk.domain.shared.entity.master.tmd;

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

@Getter
@Entity
@Comment("TMD_MI이벤트 Key Bullet")
@Table(name = "tmd_mievent_key_bulletsm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEventKeyBullet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bullet_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("키 불릿 일련번호")
    private Long bulletId;

    @Column(name = "mi_event_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("MI Event ID FK")
    private Long miEventId;

    @Column(name = "bullet_order", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("표시 순서")
    private Integer bulletOrder;

    @Column(name = "bullet_text", length = 500, nullable = false)
    @Comment("불릿 내용")
    private String bulletText;

    @Column(name = "category_type", length = 1, columnDefinition = "char(1)")
    @Comment("분류 유형 (A:사건상태, B:실물수급, C:가격영향)")
    private String categoryType;

    @Builder
    public MiEventKeyBullet(Long miEventId, Integer bulletOrder, String bulletText, String categoryType) {
        this.miEventId = miEventId;
        this.bulletOrder = bulletOrder;
        this.bulletText = bulletText;
        this.categoryType = categoryType;
    }
}
