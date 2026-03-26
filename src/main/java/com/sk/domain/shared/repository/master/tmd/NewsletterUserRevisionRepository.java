package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevision;
import com.sk.domain.shared.enums.newsletter.RevisionEditStatusCode;
import com.sk.domain.shared.enums.newsletter.RevisionSnapshotBaseType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsletterUserRevisionRepository extends JpaRepository<NewsletterUserRevision, Long> {

    Optional<NewsletterUserRevision> findByIssueOriginalAndOwnerUserUserNoAndIsDeletedFalse(
        NewsletterIssueOriginal issueOriginal,
        Integer ownerUserNo
    );

    List<NewsletterUserRevision> findAllByIssueOriginal(NewsletterIssueOriginal issueOriginal);

    @Query("""
        SELECT r
        FROM NewsletterUserRevision r
        JOIN r.issueOriginal o
        JOIN o.newsletterMaster m
        WHERE r.newsletterUserRevisionId = :revisionId
          AND r.ownerUser.userNo = :ownerUserNo
          AND r.isDeleted = false
          AND o.isDeleted = false
          AND m.newsletterMasterId = :newsletterId
          AND o.issueNo = :issueNo
        """)
    Optional<NewsletterUserRevision> findOwnedRevision(
        @Param("revisionId") Long revisionId,
        @Param("ownerUserNo") Integer ownerUserNo,
        @Param("newsletterId") Integer newsletterId,
        @Param("issueNo") Integer issueNo
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE NewsletterUserRevision r
        SET r.editStatus = :editStatus,
            r.snapshotBaseType = :snapshotBaseType,
            r.snapshotNo = :snapshotNo,
            r.deltaNo = :deltaNo,
            r.revisionNo = r.revisionNo + 1,
            r.modifyDt = :now,
            r.modifyUserId = :modifierUserId,
            r.draftSavedAt = CASE WHEN :isDraft = true THEN :now ELSE null END,
            r.finalSavedAt = CASE WHEN :isFinal = true THEN :now ELSE r.finalSavedAt END
        WHERE r.newsletterUserRevisionId = :revisionId
          AND r.ownerUser.userNo = :ownerUserNo
          AND r.revisionNo = :expectedRevisionNo
          AND r.isDeleted = false
        """)
    int updateWithExpectedRevisionNo(
        @Param("revisionId") Long revisionId,
        @Param("ownerUserNo") Integer ownerUserNo,
        @Param("expectedRevisionNo") Long expectedRevisionNo,
        @Param("editStatus") RevisionEditStatusCode editStatus,
        @Param("snapshotBaseType") RevisionSnapshotBaseType snapshotBaseType,
        @Param("snapshotNo") Long snapshotNo,
        @Param("deltaNo") Long deltaNo,
        @Param("isDraft") boolean isDraft,
        @Param("isFinal") boolean isFinal,
        @Param("modifierUserId") String modifierUserId,
        @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM NewsletterUserRevision r
        WHERE r.issueOriginal = :issueOriginal
        """)
    int deleteByIssueOriginal(@Param("issueOriginal") NewsletterIssueOriginal issueOriginal);
}
