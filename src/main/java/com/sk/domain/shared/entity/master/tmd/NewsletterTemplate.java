package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_뉴스레터템플릿")
@Table(name = "tmd_nws_ltr_tmplt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_reg_sn", nullable = false, columnDefinition = "int unsigned")
    @Comment("데이터등록일련번호")
    private Integer dataRegistrationSn;

    @Column(name = "nws_ltr_tmplt_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스레터템플릿ID")
    private Integer newsletterTemplateId;

    @Column(name = "elmnt_type", nullable = false, length = 50)
    @Comment("템플릿타입코드")
    private String elementType;

    @Column(name = "elmnt_ord", nullable = false, columnDefinition = "int")
    @ColumnDefault("1")
    @Comment("템플릿타입코드가 여러개일때 순번 정의")
    private Integer elementOrder;

    @Column(name = "elmnt_val", nullable = false, length = 200)
    @Comment("해당템플릿의 값")
    private String elementValue;
}
