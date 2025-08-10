package com.sk.domain.shared.entity.master.tmd;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.mi.dto.request.notification.NotificationSettingReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.NotiCrtvlSptCd;
import com.sk.domain.shared.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

    @Column(name = "prdc_data_yn", nullable = false, columnDefinition = "char(1)")
    @Comment("예측데이터여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isPredictData;

    @Builder
    public GoodsCriticalValueSetting(
            LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, User user, GoodsDataSourceDetail goodsDataSourceDetail,
            NotiCrtvlSptCd notificationCriticalValueSeparateCd,
            BigDecimal criticalValueSettingVol, String criticalValueSettingUnitNm, Boolean isPredictData) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.user = user;
        this.goodsDataSourceDetail = goodsDataSourceDetail;
        this.notificationCriticalValueSeparateCd = notificationCriticalValueSeparateCd;
        this.criticalValueSettingVol = criticalValueSettingVol;
        this.criticalValueSettingUnitNm = criticalValueSettingUnitNm;
        this.isPredictData = isPredictData;
    }

    public static GoodsCriticalValueSetting of(
            NotificationSettingReq.Insert dto,
            GoodsDataSourceDetail goodsDataSourceDetail,
            User user) {
        return GoodsCriticalValueSetting.builder()
                .notificationCriticalValueSeparateCd(dto.notificationCriticalValueSeparateCd())
                .criticalValueSettingVol(dto.criticalValueSettingVol())
                .criticalValueSettingUnitNm(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
                .goodsDataSourceDetail(goodsDataSourceDetail)
                .user(user)
                .isPredictData(dto.notificationType() == NotificationType.FORECAST)
                .build();
    }

    public static GoodsCriticalValueSetting of(
            NotificationSettingReq.UpdateSetting dto,
            GoodsDataSourceDetail goodsDataSourceDetail,
            User user) {
        return GoodsCriticalValueSetting.builder()
                .notificationCriticalValueSeparateCd(dto.notificationCriticalValueSeparateCd())
                .criticalValueSettingVol(dto.criticalValueSettingVol())
                .criticalValueSettingUnitNm(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
                .goodsDataSourceDetail(goodsDataSourceDetail)
                .user(user)
                .build();
    }

    public void update(NotificationSettingReq.UpdateSetting dto) {
        if (ObjectUtils.isNotEmpty(dto.notificationCriticalValueSeparateCd())) {
            this.notificationCriticalValueSeparateCd = dto.notificationCriticalValueSeparateCd();
        }

        if (ObjectUtils.isNotEmpty(dto.criticalValueSettingVol())) {
            this.criticalValueSettingVol = dto.criticalValueSettingVol();
        }
    }
}
