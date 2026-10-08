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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 사용자별 이벤트 키워드 알림 설정.
 * 키워드 정규화(trim, 연속 공백 1칸, NFC)와 길이·개수 검증은 서비스에서 수행한 뒤 생성한다.
 * 해지는 softDelete, 같은 키워드 재등록은 restore로 기존 row를 되살린다.
 */
@Getter
@Entity
@Comment("TMD_MI이벤트키워드설정기본")
@Table(name = "tmd_mi_evnt_keywdsetm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventKeywordSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keywd_set_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("키워드설정ID")
    private Long keywordSettingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", referencedColumnName = "usr_no", nullable = false)
    @Comment("사용자번호")
    private User user;

    @Column(name = "keywd_nm", nullable = false, length = 30)
    @Comment("키워드명")
    private String keywordName;

    @Column(name = "noti_use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("알림사용여부")
    @ColumnDefault("'Y'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isNotificationUse;

    public static EventKeywordSetting create(User user, String normalizedKeywordName) {
        EventKeywordSetting setting = new EventKeywordSetting();
        setting.user = user;
        setting.keywordName = normalizedKeywordName;
        setting.isNotificationUse = Boolean.TRUE;
        return setting;
    }

    public void changeNotificationUse(boolean isNotificationUse) {
        this.isNotificationUse = isNotificationUse;
    }

    /**
     * 해지했던 키워드를 다시 등록할 때 사용한다. 알림은 ON 상태로 복구하고,
     * 대소문자만 다른 입력이면 새로 입력한 표기로 바꾼다.
     */
    public void restore(String normalizedKeywordName) {
        softRestore();
        this.keywordName = normalizedKeywordName;
        this.isNotificationUse = Boolean.TRUE;
    }
}
