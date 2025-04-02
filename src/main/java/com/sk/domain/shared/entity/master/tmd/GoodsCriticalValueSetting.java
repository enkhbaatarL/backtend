package com.sk.domain.shared.entity.master.tmd;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Comment;

import com.sk.domain.mi.dto.request.notification.NotificationReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.NotiCrtvlSptCd;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_상품임계치설정")
@Table(name = "tmd_gdscrtvlsetl")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsCriticalValueSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usr_gds_noti_set_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자상품알림설정번호")
    private Integer userGoodsNotificationSettingNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", referencedColumnName = "usr_no", nullable = false)
    @Comment("사용자번호")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_src_no", referencedColumnName = "gds_data_src_no", nullable = false)
    @Comment("상품데이터소스번호")
    private GoodsDataSourceDetail goodsDataSourceDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "noti_crtvl_spt_cd", nullable = false, length = 10)
    @Comment("알림임계치구분코드")
    private NotiCrtvlSptCd notificationCriticalValueSeparateCd;

    @Column(name = "crtvl_set_vol", nullable = false, precision = 20, scale = 5)
    @Comment("임계치설정량")
    private BigDecimal criticalValueSettingVol;

    @Column(name = "crtvl_set_unit_nm", length = 20)
    @Comment("임계치설정단위명")
    private String criticalValueSettingUnitNm;

    @Builder
    public GoodsCriticalValueSetting(
            LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, User user, GoodsDataSourceDetail goodsDataSourceDetail,
            NotiCrtvlSptCd notificationCriticalValueSeparateCd,
            BigDecimal criticalValueSettingVol, String criticalValueSettingUnitNm) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.user = user;
        this.goodsDataSourceDetail = goodsDataSourceDetail;
        this.notificationCriticalValueSeparateCd = notificationCriticalValueSeparateCd;
        this.criticalValueSettingVol = criticalValueSettingVol;
        this.criticalValueSettingUnitNm = criticalValueSettingUnitNm;
    }

    public static GoodsCriticalValueSetting of(
            NotificationReq.Setting dto,
            GoodsDataSourceDetail goodsDataSourceDetail,
            User user) {
        return GoodsCriticalValueSetting.builder()
                .notificationCriticalValueSeparateCd(dto.notificationCriticalValueSeparateCd())
                .criticalValueSettingVol(dto.criticalValueSettingVol())
                .criticalValueSettingUnitNm(dto.criticalValueSettingUnitNm())
                .goodsDataSourceDetail(goodsDataSourceDetail)
                .user(user)
                .build();
    }

    public static GoodsCriticalValueSetting of(
            NotificationReq.UpdateSetting dto,
            GoodsDataSourceDetail goodsDataSourceDetail,
            User user) {
        return GoodsCriticalValueSetting.builder()
                .notificationCriticalValueSeparateCd(dto.notificationCriticalValueSeparateCd())
                .criticalValueSettingVol(dto.criticalValueSettingVol())
                .criticalValueSettingUnitNm(dto.criticalValueSettingUnitNm())
                .goodsDataSourceDetail(goodsDataSourceDetail)
                .user(user)
                .build();
    }

    public void update(NotificationReq.UpdateSetting dto) {
        if (ObjectUtils.isNotEmpty(dto.notificationCriticalValueSeparateCd())) {
            this.notificationCriticalValueSeparateCd = dto.notificationCriticalValueSeparateCd();
        }

        if (ObjectUtils.isNotEmpty(dto.criticalValueSettingVol())) {
            this.criticalValueSettingVol = dto.criticalValueSettingVol();
        }

        if (ObjectUtils.isNotEmpty(dto.criticalValueSettingUnitNm())) {
            this.criticalValueSettingUnitNm = dto.criticalValueSettingUnitNm();
        }
    }
}
