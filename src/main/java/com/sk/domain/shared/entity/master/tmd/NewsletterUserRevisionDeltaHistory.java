package com.sk.domain.shared.entity.master.tmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.newsletter.RevisionSaveRequestType;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Comment("TMD_뉴스레터사용자리비전델타이력")
@Table(
    name = "tmd_nwsltrusrrvsndltah",
    uniqueConstraints = @UniqueConstraint(
        name = "tmd_nwsltrusrrvsndltah_ux01",
        columnNames = {"nws_ltr_usr_rvsn_id", "dlta_no"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterUserRevisionDeltaHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_usr_rvsn_dlta_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터사용자리비전델타ID")
    private Long newsletterUserRevisionDeltaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_usr_rvsn_id", nullable = false, referencedColumnName = "nws_ltr_usr_rvsn_id")
    @Comment("뉴스레터사용자리비전ID")
    private NewsletterUserRevision newsletterUserRevision;

    @Column(name = "dlta_no", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("델타번호(리비전 내 단조증가)")
    private Long deltaNo;

    @Column(name = "base_rvsn_no", columnDefinition = "bigint unsigned")
    @Comment("요청기준리비전번호(revisionNo, optional)")
    private Long baseRevisionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "req_type", nullable = false, length = 10)
    @Comment("요청유형(DRAFT,SAVE)")
    private RevisionSaveRequestType requestType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dlta_json", columnDefinition = "json", nullable = false)
    @Comment("델타JSON(JSON Patch 배열)")
    private JsonNode deltaJson;

    public static NewsletterUserRevisionDeltaHistory create(
        NewsletterUserRevision revision,
        long deltaNo,
        Long baseRevisionNo,
        RevisionSaveRequestType requestType,
        JsonNode deltaJson
    ) {
        NewsletterUserRevisionDeltaHistory history = new NewsletterUserRevisionDeltaHistory();
        history.newsletterUserRevision = revision;
        history.deltaNo = deltaNo;
        history.baseRevisionNo = baseRevisionNo;
        history.requestType = requestType;
        history.deltaJson = deltaJson;
        return history;
    }
}
