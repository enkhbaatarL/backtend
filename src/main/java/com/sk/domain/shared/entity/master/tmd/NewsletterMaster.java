package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.newsletter.PeriodType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_뉴스레터마스터")
@Table(name = "tmd_nws_ltr_mstr")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_mstr_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스레터마스터ID")
    private Integer newsletterMasterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "perd_type", nullable = false, length = 20)
    @Comment("기간을 나타내는 타입 DAILY, WEEKLY, ON-DEMAND형")
    private PeriodType periodType;

    @Column(name = "expn", nullable = false, length = 100)
    @Comment("뉴스레터마스터 설명")
    private String explanation;

    @Column(name = "img_url", length = 500)
    @Comment("보여줄 이미지 주소")
    private String imageUrl;

    @Column(name = "nws_ltr_tmplt_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스레터템플릿ID")
    private Integer newsletterTemplateId;

    @Column(name = "mstr_nm", nullable = false, length = 100)
    @Comment("뉴스레터마스터 이름")
    private String masterName;

    @OneToMany(mappedBy = "newsletterMaster", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("issueNo DESC")
    private List<NewsletterPublishHistory> publishHistories = new ArrayList<>();

    @Builder
    public NewsletterMaster(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
        String modifyUserId,
        Boolean isDeleted, Integer newsletterMasterId, PeriodType periodType, String explanation,
        String imageUrl, Integer newsletterTemplateId, String masterName) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.newsletterMasterId = newsletterMasterId;
        this.periodType = periodType;
        this.explanation = explanation;
        this.imageUrl = imageUrl;
        this.newsletterTemplateId = newsletterTemplateId;
        this.masterName = masterName;
    }

    public static NewsletterMaster of(PeriodType periodType, String explanation, String imageUrl,
        Integer newsletterTemplateId, String masterName) {
        return NewsletterMaster.builder()
            .periodType(periodType)
            .explanation(explanation)
            .imageUrl(imageUrl)
            .newsletterTemplateId(newsletterTemplateId)
            .masterName(masterName)
            .build();
    }

    public void update(PeriodType periodType, String explanation, String imageUrl,
        String masterName) {
        if (ObjectUtils.isNotEmpty(periodType)) {
            this.periodType = periodType;
        }

        if (StringUtils.isNotEmpty(explanation)) {
            this.explanation = explanation;
        }

        if (StringUtils.isNotEmpty(imageUrl)) {
            this.imageUrl = imageUrl;
        }

        if (StringUtils.isNotEmpty(masterName)) {
            this.masterName = masterName;
        }
    }

    public boolean isActive() {
        return !this.getIsDeleted();
    }
}
