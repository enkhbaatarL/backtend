package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.newsletter.RevisionEditStatusCode;
import com.sk.domain.shared.enums.newsletter.RevisionSnapshotBaseType;
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

@Getter
@Entity
@Comment("TMD_뉴스레터사용자리비전마스터")
@Table(
    name = "tmd_nwsltrusrrvsnm",
    uniqueConstraints = @UniqueConstraint(
        name = "tmd_nwsltrusrrvsnm_ux01",
        columnNames = {"nws_ltr_org_iss_id", "ownr_usr_no"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterUserRevision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_usr_rvsn_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터사용자리비전ID")
    private Long newsletterUserRevisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_org_iss_id", nullable = false, referencedColumnName = "nws_ltr_org_iss_id")
    @Comment("뉴스레터원본이슈ID")
    private NewsletterIssueOriginal issueOriginal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownr_usr_no", nullable = false, referencedColumnName = "usr_no")
    @Comment("소유자사용자번호")
    private User ownerUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "rvsn_stat_cd", nullable = false, length = 20)
    @Comment("편집상태(EDIT_DRAFT,EDIT_SAVED)")
    private RevisionEditStatusCode editStatus;

    @Column(name = "rvsn_no", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("리비전번호(1 이상)")
    private Long revisionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "snpsht_base_type", nullable = false, length = 30)
    @Comment("스냅샷기준유형(ORIGINAL,REVISION_SNAPSHOT)")
    private RevisionSnapshotBaseType snapshotBaseType;

    @Column(name = "snpsht_no", columnDefinition = "bigint unsigned")
    @Comment("최신스냅샷번호")
    private Long snapshotNo;

    @Column(name = "dlta_no", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("마지막델타번호")
    private Long deltaNo;

    @Column(name = "fnsv_dtm")
    @Comment("최종저장일시")
    private LocalDateTime finalSavedAt;

    @Column(name = "drft_strg_dtm")
    @Comment("최근임시저장일시")
    private LocalDateTime draftSavedAt;

    public static NewsletterUserRevision create(
        NewsletterIssueOriginal issueOriginal,
        User ownerUser,
        RevisionEditStatusCode editStatus,
        long revisionNo,
        long deltaNo,
        LocalDateTime now
    ) {
        NewsletterUserRevision revision = new NewsletterUserRevision();
        revision.issueOriginal = issueOriginal;
        revision.ownerUser = ownerUser;
        revision.editStatus = editStatus;
        revision.revisionNo = revisionNo;
        revision.snapshotBaseType = RevisionSnapshotBaseType.ORIGINAL;
        revision.snapshotNo = null;
        revision.deltaNo = deltaNo;
        if (editStatus == RevisionEditStatusCode.EDIT_DRAFT) {
            revision.draftSavedAt = now;
        } else {
            revision.finalSavedAt = now;
        }
        return revision;
    }

    public void applySave(
        RevisionEditStatusCode nextStatus,
        long nextRevisionNo,
        long nextDeltaNo,
        boolean snapshotSaved,
        long nextSnapshotNo,
        LocalDateTime now
    ) {
        this.editStatus = nextStatus;
        this.revisionNo = nextRevisionNo;
        this.deltaNo = nextDeltaNo;

        if (nextStatus == RevisionEditStatusCode.EDIT_DRAFT) {
            this.draftSavedAt = now;
        } else {
            this.finalSavedAt = now;
        }

        if (snapshotSaved) {
            this.snapshotBaseType = RevisionSnapshotBaseType.REVISION_SNAPSHOT;
            this.snapshotNo = nextSnapshotNo;
        }
    }
}
