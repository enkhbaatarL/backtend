package com.sk.domain.shared.entity.master.tmd;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("뉴스레터이벤트")
@Table(name = "tmd_nws_ltr_txt",
    uniqueConstraints = @UniqueConstraint(name = "tmd_nws_ltr_txt_uk01",
        columnNames = {"nws_ltr_pub_id", "nws_ltr_txt_type"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterText extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_txt_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스레터이벤트ID")
    private Integer newsletterTextId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_pub_id", nullable = false, referencedColumnName = "nws_ltr_pub_id")
    @Comment("뉴스레터발행ID")
    private NewsletterPublishHistory newsletterPublishHistory;

    @Column(name = "nws_ltr_txt_type", nullable = false, length = 20)
    @Comment("뉴스레터 글 타입")
    private String newsletterTextType;

    @Column(name = "nws_ltr_txt_value", columnDefinition = "TEXT")
    @Comment("뉴스레터 글 내용")
    private String newsletterTextValue;

    @Builder
    public NewsletterText(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
        String modifyUserId, Boolean isDeleted, Integer newsletterTextId,
        NewsletterPublishHistory newsletterPublishHistory, String newsletterTextType,
        String newsletterTextValue) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.newsletterTextId = newsletterTextId;
        this.newsletterPublishHistory = newsletterPublishHistory;
        this.newsletterTextType = newsletterTextType;
        this.newsletterTextValue = newsletterTextValue;
    }
}
