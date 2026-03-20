package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.newsletter.PublishStatusCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_뉴스레터발행이력")
@Table(name = "tmd_nws_ltr_pub_hist",
       uniqueConstraints = @UniqueConstraint(name = "tmd_nws_ltr_pub_hist_ux01",
                                           columnNames = {"nws_ltr_mstr_id", "issno"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterPublishHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_pub_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스레터발행ID")
    private Integer newsletterPublishId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_mstr_id", nullable = false, referencedColumnName = "nws_ltr_mstr_id")
    @Comment("뉴스레터마스터ID")
    private NewsletterMaster newsletterMaster;

    @Column(name = "nws_ltr_tmplt_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("발행시점뉴스레터템플릿버전ID")
    private Long newsletterTemplateId;

    @Column(name = "issno", nullable = false)
    @Comment("발행차수")
    private Integer issueNo;

    @Column(name = "nws_ltr_titl", length = 100)
    @Comment("뉴스레터제목")
    private String newsletterTitle;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "pub_stat_cd")
    @Comment("발행상태 PUBLISHING, PUBLISHED, SUSPENDED")
    private PublishStatusCode publishStatusCode;

    @Column(name = "pub_dtm")
    @Comment("발행일자")
    private LocalDateTime publishDateTime;

    @Lob
    @Column(name = "ai_smry", columnDefinition = "TEXT")
    @Comment("AI요약")
    private String aiSummary;

    @Column(name = "eml_titl", length = 100)
    @Comment("이메일 제목")
    private String emailSubject;
}
