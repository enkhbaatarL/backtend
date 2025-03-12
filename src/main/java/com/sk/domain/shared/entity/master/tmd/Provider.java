package com.sk.domain.shared.entity.master.tmd;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
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

@Entity
@Table(name = "tmd_prvdm")
@Comment("TMD_프로바이더기본")
@Getter
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

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Column(name = "prvd_expn", length = 500)
    @Comment("프로바이더설명")
    private String providerExpn;

    @Builder
    public Provider(Integer providerNo, String providerNm, String providerCd, Boolean isUsed,
            String providerExpn) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.providerCd = providerCd;
        this.isUsed = isUsed;
        this.providerExpn = providerExpn;
    }
}
