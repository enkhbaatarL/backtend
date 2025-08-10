package com.sk.domain.shared.entity.master.tmd;

import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmd.UserHistoryId;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_사용자이력")
@Table(name = "tmd_usrh")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHistory extends BaseEntity {

    @EmbeddedId
    private UserHistoryId id;

    @MapsId("userNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", referencedColumnName = "usr_no")
    private User user;

    @Column(name = "otcmp_svc_accs_psbl_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("타사서비스접근가능여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isOtherCompanyServiceAccessPossible;

    @Builder
    public UserHistory(UserHistoryId id, Boolean isOtherCompanyServiceAccessPossible, User user) {
        this.id = id;
        this.isOtherCompanyServiceAccessPossible = isOtherCompanyServiceAccessPossible;
        this.user = user;
    }
}
