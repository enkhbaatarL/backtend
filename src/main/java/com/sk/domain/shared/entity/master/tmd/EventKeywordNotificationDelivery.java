package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.notification.NotificationChannel;
import com.sk.domain.shared.enums.notification.NotificationSendStatus;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 이벤트 키워드 알림 발송 내역. 한 row = 알림 1건 x 채널 1개, 재시도는 같은 row를 갱신한다.
 * 수신 주소(이메일·전화번호)는 저장하지 않으며, 오류 메시지는 호출자가 수신 주소를 마스킹한 뒤 넘긴다.
 */
@Getter
@Entity
@Comment("TMD_MI이벤트키워드발송내역")
@Table(name = "tmd_mi_evnt_keywdsendl")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventKeywordNotificationDelivery extends BaseEntity {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keywd_send_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("키워드발송ID")
    private Long keywordSendId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keywd_noti_id", referencedColumnName = "keywd_noti_id", nullable = false)
    @Comment("키워드알림ID")
    private EventKeywordNotification notification;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_chnl_cd", nullable = false, length = 20)
    @Comment("발송채널코드")
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_stat_cd", nullable = false, length = 20)
    @Comment("발송상태코드")
    @ColumnDefault("'PENDING'")
    private NotificationSendStatus status;

    @Column(name = "send_try_cnt", nullable = false, columnDefinition = "int unsigned")
    @Comment("발송시도수")
    @ColumnDefault("0")
    private Integer sendTryCount;

    @Column(name = "send_dtm")
    @Comment("발송일시")
    private LocalDateTime sendDtm;

    @Column(name = "err_msg_cntt", length = ERROR_MESSAGE_MAX_LENGTH)
    @Comment("오류메세지내용")
    private String errorMessage;

    public static EventKeywordNotificationDelivery createPending(EventKeywordNotification notification, NotificationChannel channel) {
        EventKeywordNotificationDelivery delivery = new EventKeywordNotificationDelivery();
        delivery.notification = notification;
        delivery.channel = channel;
        delivery.status = NotificationSendStatus.PENDING;
        delivery.sendTryCount = 0;
        return delivery;
    }

    /** 선점(SENDING)한 디스패처만 결과를 기록한다. 그 사이 SENDING 타임아웃 복구로 상태가 바뀌었으면 false */
    public boolean isSending() {
        return this.status == NotificationSendStatus.SENDING;
    }

    public void markSuccess() {
        recordAttempt();
        this.status = NotificationSendStatus.SUCCESS;
        this.errorMessage = null;
    }

    /**
     * @param maskedErrorMessage 수신 주소 등 개인정보를 마스킹한 오류 메시지
     */
    public void markFail(String maskedErrorMessage) {
        recordAttempt();
        this.status = NotificationSendStatus.FAIL;
        this.errorMessage = StringUtils.abbreviate(maskedErrorMessage, ERROR_MESSAGE_MAX_LENGTH);
    }

    private void recordAttempt() {
        this.sendTryCount = this.sendTryCount + 1;
        this.sendDtm = LocalDateTime.now();
    }
}
