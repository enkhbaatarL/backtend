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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_뉴스레터구독마스터")
@Table(
    name = "tmd_nwsltrsubm",
    uniqueConstraints = @UniqueConstraint(
        name = "tmd_nwsltrsubm_ux01",
        columnNames = {"nws_ltr_mstr_id", "usr_no"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_sub_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터구독ID")
    private Long newsletterSubscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_mstr_id", nullable = false, referencedColumnName = "nws_ltr_mstr_id")
    @Comment("뉴스레터마스터ID")
    private NewsletterMaster newsletterMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", nullable = false, referencedColumnName = "usr_no")
    @Comment("사용자번호")
    private User user;

    @Column(name = "sub_yn", nullable = false)
    @Comment("구독여부")
    private Boolean isSubscribed;

    @Column(name = "sub_dtm")
    @Comment("구독일시")
    private LocalDateTime subscribedAt;

    @Column(name = "unsub_dtm")
    @Comment("구독해지일시")
    private LocalDateTime unsubscribedAt;

    public static NewsletterSubscription subscribe(NewsletterMaster newsletterMaster,
        User actorUser) {
        NewsletterSubscription subscription = new NewsletterSubscription();
        subscription.newsletterMaster = newsletterMaster;
        subscription.user = actorUser;
        subscription.isSubscribed = true;
        subscription.subscribedAt = LocalDateTime.now();
        subscription.unsubscribedAt = null;
        subscription.softRestore();
        return subscription;
    }

    public void subscribe() {
        if (Boolean.TRUE.equals(this.isSubscribed) && !this.getIsDeleted()) {
            return;
        }

        this.isSubscribed = true;
        this.subscribedAt = this.subscribedAt == null ? LocalDateTime.now() : this.subscribedAt;
        this.unsubscribedAt = null;
        this.softRestore();
    }

    public void unsubscribe() {
        if (Boolean.FALSE.equals(this.isSubscribed) && !this.getIsDeleted()) {
            return;
        }

        this.isSubscribed = false;
        this.unsubscribedAt = LocalDateTime.now();
        this.softRestore();
    }
}
