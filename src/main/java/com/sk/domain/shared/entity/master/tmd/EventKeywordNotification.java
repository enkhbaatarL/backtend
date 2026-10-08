package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 이벤트 키워드 알림 내역. 한 row = 사용자 x 이벤트 x 키워드 (My Notifications 1행).
 * 키워드명·이벤트명(one_line_summary)은 매칭 시점 스냅샷이며, mi_event_id는 물리 FK 없는 논리참조다.
 */
@Getter
@Entity
@Comment("TMD_MI이벤트키워드알림내역")
@Table(name = "tmd_mi_evnt_keywdnotil")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventKeywordNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keywd_noti_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("키워드알림ID")
    private Long keywordNotificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", referencedColumnName = "usr_no", nullable = false)
    @Comment("사용자번호")
    private User user;

    @Column(name = "mi_event_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("MI이벤트ID")
    private Long miEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keywd_set_id", referencedColumnName = "keywd_set_id", nullable = false)
    @Comment("키워드설정ID")
    private EventKeywordSetting keywordSetting;

    @Column(name = "keywd_snpsht_nm", nullable = false, length = 30)
    @Comment("키워드스냅샷명")
    private String keywordSnapshotName;

    @Column(name = "evnt_snpsht_nm", nullable = false, length = 300)
    @Comment("이벤트스냅샷명")
    private String eventSnapshotName;

    @Column(name = "inqr_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("조회여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isInquiry;

    @Column(name = "inqr_dtm")
    @Comment("조회일시")
    private LocalDateTime inquiryDtm;

    public static EventKeywordNotification create(EventKeywordSetting keywordSetting, Long miEventId,
            String eventName) {
        EventKeywordNotification notification = new EventKeywordNotification();
        notification.user = keywordSetting.getUser();
        notification.miEventId = miEventId;
        notification.keywordSetting = keywordSetting;
        notification.keywordSnapshotName = keywordSetting.getKeywordName();
        notification.eventSnapshotName = eventName;
        notification.isInquiry = Boolean.FALSE;
        return notification;
    }

    /**
     * 읽음 처리. inqr_yn과 inqr_dtm을 함께 갱신하며(ck01), 최초 조회 시각은 이후 바뀌지 않는다.
     */
    public void markAsRead() {
        if (Boolean.TRUE.equals(this.isInquiry)) {
            return;
        }
        this.isInquiry = Boolean.TRUE;
        this.inquiryDtm = LocalDateTime.now();
    }
}
