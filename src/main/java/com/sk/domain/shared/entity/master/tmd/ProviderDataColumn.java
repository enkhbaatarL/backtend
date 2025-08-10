package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmd.ProviderProviderDataColumnMappingId;

import jakarta.persistence.Column;
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
@Comment("TMD_프로바이더데이터컬럼내역")
@Table(name = "tmd_prvddatacolml")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProviderDataColumn extends BaseEntity {

    @EmbeddedId
    private ProviderProviderDataColumnMappingId id;

    @Column(name = "data_colm_expn", nullable = false, length = 500)
    @Comment("데이터컬럼설명")
    private String dataColumnExpn;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("providerNo") // 복합키의 providerNo와 연관관계를 연결합니다.
    @JoinColumn(name = "prvd_no", nullable = false, referencedColumnName = "prvd_no")
    @Comment("프로바이더번호")
    private Provider provider;

    @Builder
    public ProviderDataColumn(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            ProviderProviderDataColumnMappingId id, String dataColumnExpn, Provider provider) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.dataColumnExpn = dataColumnExpn;
        this.provider = provider;
    }

    public static ProviderDataColumn of(Provider provider, String providerDataColumnNm, String dataColumnExpn) {
        return ProviderDataColumn.builder()
                .id(ProviderProviderDataColumnMappingId.of(provider.getProviderNo(), providerDataColumnNm))
                .dataColumnExpn(dataColumnExpn)
                .provider(provider)
                .build();
    }
}
