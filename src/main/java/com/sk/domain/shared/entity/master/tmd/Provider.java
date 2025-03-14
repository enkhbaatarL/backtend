package com.sk.domain.shared.entity.master.tmd;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.admin.dto.request.ProviderReq;
import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_프로바이더기본")
@Table(name = "tmd_prvdm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Provider extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prvd_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("프로바이더번호")
    private Integer providerNo;

    @Column(name = "prvd_nm", nullable = false, length = 100)
    @Comment("프로바이더명")
    private String providerNm;

    @Column(name = "prvd_cd", nullable = false, length = 20)
    @Comment("프로바이더코드")
    private String providerCd;

    @Column(name = "prvd_expn", length = 500)
    @Comment("프로바이더설명")
    private String providerExpn;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Builder
    public Provider(Integer providerNo, String providerNm, String providerCd, String providerExpn,
            Boolean isUsed) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.providerCd = providerCd;
        this.providerExpn = providerExpn;
        this.isUsed = isUsed;
    }

    public static Provider of(ProviderReq.Create providerReq) {
        return Provider.builder()
                .providerNm(providerReq.providerNm())
                .providerCd(providerReq.providerCd())
                .providerExpn(providerReq.providerExpn())
                .isUsed(providerReq.isUsed())
                .build();
    }

    public void update(ProviderReq.Update providerReq) {

        if (StringUtils.isNotEmpty(providerReq.providerNm())) {
            this.providerNm = providerReq.providerNm();
        }

        if (StringUtils.isNotEmpty(providerReq.providerCd())) {
            this.providerCd = providerReq.providerCd();
        }

        if (StringUtils.isNotEmpty(providerReq.providerExpn())) {
            this.providerExpn = providerReq.providerExpn();
        }

        if (ObjectUtils.isNotEmpty(providerReq.isUsed())) {
            this.isUsed = providerReq.isUsed();
        }
    }

    public void softDelete() {
        super.softDelete();
    }
}