package com.sk.domain.shared.repository.master.tmd;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSource;
import com.sk.domain.shared.entity.master.tmd.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "tmd_gdscrtvlsetl", indexes = {
        @Index(name = "tmd_gdscrtvlsetl_fk01", columnList = "usr_no"),
        @Index(name = "tmd_gdscrtvlsetl_fk02", columnList = "gds_data_src_no")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsCriticalValueSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usr_gds_noti_set_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자상품알림설정번호")
    private Integer userGoodsNotificationSettingNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", nullable = false)
    @Comment("사용자번호")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_src_no", nullable = false)
    @Comment("상품데이터소스번호")
    private GoodsDataSource goodsDataSource;

    @Column(name = "noti_crtvl_spt_cd", nullable = false, length = 10)
    @Comment("알림임계치구분코드")
    private String notificationCriticalSupportCd;

    @Column(name = "crtvl_set_vol", nullable = false, precision = 20, scale = 5)
    @Comment("임계치설정량")
    private BigDecimal criticalValueSettingVol;

    @Column(name = "crtvl_set_unit_nm", length = 20)
    @Comment("임계치설정단위명")
    private String criticalValueSettingUnitNm;

    @Builder
    public GoodsCriticalValueSetting(
            LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, User user, GoodsDataSource goodsDataSource,
            String notificationCriticalSupportCd,
            BigDecimal criticalValueSettingVol, String criticalValueSettingUnitNm) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.user = user;
        this.goodsDataSource = goodsDataSource;
        this.notificationCriticalSupportCd = notificationCriticalSupportCd;
        this.criticalValueSettingVol = criticalValueSettingVol;
        this.criticalValueSettingUnitNm = criticalValueSettingUnitNm;
    }

    public static GoodsCriticalValueSetting of(
            User user, GoodsDataSource goodsDataSource,
            String notificationCriticalSupportCd,
            BigDecimal criticalValueSettingVol, String criticalValueSettingUnitNm) {
        return GoodsCriticalValueSetting.builder()
                .user(user)
                .goodsDataSource(goodsDataSource)
                .notificationCriticalSupportCd(notificationCriticalSupportCd)
                .criticalValueSettingVol(criticalValueSettingVol)
                .criticalValueSettingUnitNm(criticalValueSettingUnitNm)
                .build();
    }
}