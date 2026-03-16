package com.sk.domain.shared.entity.master.tmd;

import com.fasterxml.jackson.databind.JsonNode;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Comment("TMD_뉴스레터사용자리비전스냅샷")
@Table(
    name = "tmd_nwsltrusrrvsnsnpshth",
    uniqueConstraints = @UniqueConstraint(
        name = "tmd_nwsltrusrrvsnsnpshth_ux01",
        columnNames = {"nws_ltr_usr_rvsn_id", "snpsht_no"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterUserRevisionSnapshotHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_usr_rvsn_snpsht_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터사용자리비전스냅샷ID")
    private Long newsletterUserRevisionSnapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_usr_rvsn_id", nullable = false, referencedColumnName = "nws_ltr_usr_rvsn_id")
    @Comment("뉴스레터사용자리비전ID")
    private NewsletterUserRevision newsletterUserRevision;

    @Column(name = "snpsht_no", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("스냅샷번호(리비전 내 단조증가)")
    private Long snapshotNo;

    @Column(name = "base_dlta_no", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("기준델타번호(복원 시작점)")
    private Long baseDeltaNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snpsht_json", columnDefinition = "json", nullable = false)
    @Comment("스냅샷JSON")
    private JsonNode snapshotJson;

    @Column(name = "schm_ver", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("스키마버전")
    private Integer schemaVersion;

    public static NewsletterUserRevisionSnapshotHistory create(
        NewsletterUserRevision revision,
        long snapshotNo,
        long baseDeltaNo,
        JsonNode snapshotJson,
        int schemaVersion
    ) {
        NewsletterUserRevisionSnapshotHistory snapshot = new NewsletterUserRevisionSnapshotHistory();
        snapshot.newsletterUserRevision = revision;
        snapshot.snapshotNo = snapshotNo;
        snapshot.baseDeltaNo = baseDeltaNo;
        snapshot.snapshotJson = snapshotJson;
        snapshot.schemaVersion = schemaVersion;
        return snapshot;
    }
}
