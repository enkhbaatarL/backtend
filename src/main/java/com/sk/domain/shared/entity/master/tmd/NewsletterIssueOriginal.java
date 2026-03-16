package com.sk.domain.shared.entity.master.tmd;

import com.fasterxml.jackson.databind.JsonNode;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Comment("TMD_뉴스레터원본이슈마스터")
@Table(
    name = "tmd_nwsltrorgissm",
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nwsltrorgissm_ux01", columnNames = {"nws_ltr_mstr_id", "issno"}),
        @UniqueConstraint(name = "tmd_nwsltrorgissm_ux02", columnNames = {"src_nws_ltr_pub_id"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterIssueOriginal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_org_iss_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터원본이슈ID")
    private Long newsletterIssueOriginalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_mstr_id", nullable = false, referencedColumnName = "nws_ltr_mstr_id")
    @Comment("뉴스레터마스터ID")
    private NewsletterMaster newsletterMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "src_nws_ltr_pub_id", nullable = false, referencedColumnName = "nws_ltr_pub_id")
    @Comment("원본발행이력ID")
    private NewsletterPublishHistory sourceNewsletterPublishHistory;

    @Column(name = "issno", nullable = false, columnDefinition = "int unsigned")
    @Comment("발행차수")
    private Integer issueNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "pub_stat_cd", nullable = false, length = 20)
    @Comment("발행상태코드(PUBLISHING,PUBLISHED,SUSPENDED)")
    private PublishStatusCode publishStatusCode;

    @Column(name = "pub_dtm")
    @Comment("발행일시")
    private LocalDateTime publishDateTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "org_snpsht_json", columnDefinition = "json", nullable = false)
    @Comment("원본스냅샷JSON")
    private JsonNode originalSnapshotJson;

    @Column(name = "schm_ver", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("스키마버전")
    private Integer schemaVersion;

    public static NewsletterIssueOriginal create(
        NewsletterMaster newsletterMaster,
        NewsletterPublishHistory sourceNewsletterPublishHistory,
        Integer issueNo,
        PublishStatusCode publishStatusCode,
        LocalDateTime publishDateTime,
        JsonNode originalSnapshotJson,
        Integer schemaVersion
    ) {
        NewsletterIssueOriginal issueOriginal = new NewsletterIssueOriginal();
        issueOriginal.newsletterMaster = newsletterMaster;
        issueOriginal.sourceNewsletterPublishHistory = sourceNewsletterPublishHistory;
        issueOriginal.issueNo = issueNo;
        issueOriginal.publishStatusCode = publishStatusCode;
        issueOriginal.publishDateTime = publishDateTime;
        issueOriginal.originalSnapshotJson = originalSnapshotJson;
        issueOriginal.schemaVersion = schemaVersion;
        return issueOriginal;
    }

    public NewsletterIssueOriginal updateSnapshot(
        JsonNode originalSnapshotJson,
        Integer schemaVersion,
        PublishStatusCode publishStatusCode,
        LocalDateTime publishDateTime,
        NewsletterPublishHistory sourceNewsletterPublishHistory
    ) {
        this.originalSnapshotJson = originalSnapshotJson;
        this.schemaVersion = schemaVersion;
        this.publishStatusCode = publishStatusCode;
        this.publishDateTime = publishDateTime;
        this.sourceNewsletterPublishHistory = sourceNewsletterPublishHistory;
        return this;
    }
}
