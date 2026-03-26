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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("뉴스레터이벤트")
@Table(name = "tmd_nws_ltr_evnt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_evnt_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스레터이벤트ID")
    private Integer newsletterEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_pub_id", nullable = false, referencedColumnName = "nws_ltr_pub_id")
    @Comment("뉴스레터발행ID")
    private NewsletterPublishHistory newsletterPublishHistory;

    @Column(name = "evnt_crt_tm", nullable = false)
    @Comment("이벤트발생시간")
    private LocalDateTime eventCreateTime;

    @Column(name = "evnt_mark_tm", nullable = false, length = 10)
    @Comment("이벤트표시시간")
    private String eventMarkTime;

    @Column(name = "trnngpnt_mark_yn", nullable = false, columnDefinition = "char(1)")
    @ColumnDefault("'N'")
    @Comment("변곡점표시여부")
    private String turningPointMarkYn;

    @Column(name = "drctnlt_type", length = 5)
    @Comment("방향성 ( UP, DOWN, NONE )")
    private String directionalityType;

    @Column(name = "evnt_titl", length = 200)
    @Comment("이벤트 제목")
    private String eventTitle;

    @Column(name = "evnt_cntt", nullable = false, length = 1000)
    @Comment("이벤트 내용")
    private String eventContent;

    @Column(name = "evnt_prvd", nullable = false, length = 100)
    @Comment("이벤트 출처")
    private String eventProvider;

    @Column(name = "org_url", columnDefinition = "TEXT")
    @Comment("원문URL")
    private String originalUrl;

    @Builder
    public NewsletterEvent(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer newsletterEventId, NewsletterPublishHistory newsletterPublishHistory,
            LocalDateTime eventCreateTime, String eventMarkTime, String turningPointMarkYn,
            String directionalityType, String eventTitle, String eventContent, String eventProvider,
            String originalUrl) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.newsletterEventId = newsletterEventId;
        this.newsletterPublishHistory = newsletterPublishHistory;
        this.eventCreateTime = eventCreateTime;
        this.eventMarkTime = eventMarkTime;
        this.turningPointMarkYn = turningPointMarkYn;
        this.directionalityType = directionalityType;
        this.eventTitle = eventTitle;
        this.eventContent = eventContent;
        this.eventProvider = eventProvider;
        this.originalUrl = originalUrl;
    }

    public static NewsletterEvent createNewsletterEvent(NewsletterPublishHistory newsletterPublishHistory,
            LocalDateTime eventCreateTime, String eventMarkTime, String turningPointMarkYn,
            String directionalityType, String eventTitle, String eventContent, String eventProvider,
            String originalUrl) {
        return NewsletterEvent.builder()
                .newsletterPublishHistory(newsletterPublishHistory)
                .eventCreateTime(eventCreateTime)
                .eventMarkTime(eventMarkTime)
                .turningPointMarkYn(turningPointMarkYn)
                .directionalityType(directionalityType)
                .eventTitle(eventTitle)
                .eventContent(eventContent)
                .eventProvider(eventProvider)
                .originalUrl(originalUrl)
                .build();
    }
}
