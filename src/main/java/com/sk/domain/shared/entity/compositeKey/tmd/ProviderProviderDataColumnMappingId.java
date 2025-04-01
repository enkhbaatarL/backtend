package com.sk.domain.shared.entity.compositeKey.tmd;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ProviderProviderDataColumnMappingId {

    @Column(name = "prvd_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("프로바이더번호")
    private Integer providerNo;

    @Column(name = "prvd_data_colm_nm", nullable = false, length = 50)
    @Comment("프로바이더데이터컬럼명")
    private String providerDataColumnNm;

    @Builder
    public ProviderProviderDataColumnMappingId(Integer providerNo, String providerDataColumnNm) {
        this.providerNo = providerNo;
        this.providerDataColumnNm = providerDataColumnNm;
    }

    public static ProviderProviderDataColumnMappingId of(Integer providerNo, String providerDataColumnNm) {
        return ProviderProviderDataColumnMappingId.builder()
                .providerNo(providerNo)
                .providerDataColumnNm(providerDataColumnNm)
                .build();
    }

}
