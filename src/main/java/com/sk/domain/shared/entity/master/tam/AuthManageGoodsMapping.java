package com.sk.domain.shared.entity.master.tam;

import java.time.LocalDateTime;

import com.sk.domain.admin.dto.request.company.AuthGoodsMapReq;
import com.sk.domain.shared.enums.GoodsAuthCd;
import jakarta.persistence.*;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.User;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tam_authmnagtgdsmappl", indexes = {
    @Index(name = "TAM_AUTHMNAGTGDSMAPPL_IX01", columnList = "usr_no"),
    @Index(name = "TAM_AUTHMNAGTGDSMAPPL_IX02", columnList = "cust_cpny_no,gds_no,gds_auth_mnagt_spt_cd")
})
@Comment("TAM_권한주체상품매핑내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthManageGoodsMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_mnagt_gds_mapp_sn", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("권한주체상품매핑일련번호")
    private Long authManagerGoodsMappingSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", referencedColumnName = "usr_no")
    @Comment("사용자")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "cust_cpny_no", referencedColumnName = "cust_cpny_no", nullable = false),
        @JoinColumn(name = "gds_no", referencedColumnName = "gds_no", nullable = false),
        @JoinColumn(name = "gds_auth_mnagt_spt_cd", referencedColumnName = "gds_auth_mnagt_spt_cd", nullable = false)
    })
    @Comment("고객회사상품매핑")
    private CustomerCompanyGoodsMapping customerCompanyGoodsMapping;

    @Enumerated(EnumType.STRING)
    @Column(name = "gds_auth_cd", nullable = false, length = 20)
    @Comment("상품권한코드")
    private GoodsAuthCd goodsAuthCd;

    @Column(name = "use_bgn_dt", nullable = false, length = 8)
    @Comment("사용시작일자")
    private String useBeginDate;

    @Column(name = "use_end_dt", nullable = false, length = 8)
    @Comment("사용종료일자")
    private String useEndDate;

    @Builder
    public AuthManageGoodsMapping(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
                                  String modifyUserId, Boolean isDeleted, Long authManagerGoodsMappingSeq, User user,
                                  CustomerCompanyGoodsMapping customerCompanyGoodsMapping, GoodsAuthCd goodsAuthCd,
                                  String useBeginDate, String useEndDate) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.authManagerGoodsMappingSeq = authManagerGoodsMappingSeq;
        this.user = user;
        this.customerCompanyGoodsMapping = customerCompanyGoodsMapping;
        this.goodsAuthCd = goodsAuthCd;
        this.useBeginDate = useBeginDate;
        this.useEndDate = useEndDate;
    }

    public static AuthManageGoodsMapping of(AuthGoodsMapReq.CompanySetting dto, GoodsAuthCd goodsAuthCd, CustomerCompanyGoodsMapping ccgMappings) {
        return AuthManageGoodsMapping.builder()
            .customerCompanyGoodsMapping(ccgMappings)
            .goodsAuthCd(goodsAuthCd)
            .useBeginDate(dto.usageBeginDate())
            .useEndDate(dto.usageEndDate())
            .build();
    }

    public static AuthManageGoodsMapping of(User user, AuthGoodsMapReq.IndividualSetting dto, GoodsAuthCd goodsAuthCd, CustomerCompanyGoodsMapping ccgMappings) {
        return AuthManageGoodsMapping.builder()
            .user(user)
            .customerCompanyGoodsMapping(ccgMappings)
            .goodsAuthCd(goodsAuthCd)
            .useBeginDate(dto.usageBeginDate())
            .useEndDate(dto.usageEndDate())
            .build();
    }

    public void update(AuthGoodsMapReq.AuthSetting setting) {
        if (ObjectUtils.isNotEmpty(setting.usageBeginDate())) {
            this.useBeginDate = setting.usageBeginDate();
        }

        if (ObjectUtils.isNotEmpty(setting.usageEndDate())) {
            this.useEndDate = setting.usageEndDate();
        }
    }

}
