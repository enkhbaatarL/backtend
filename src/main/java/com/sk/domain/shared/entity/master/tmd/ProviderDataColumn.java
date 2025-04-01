package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import com.sk.domain.shared.listener.ProviderListener;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmd.ProviderProviderDataColumnMappingId;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_프로바이더데이터컬럼내역")
@Table(name = "tmd_prvddatacolml")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(ProviderListener.class)
public class ProviderDataColumn extends BaseEntity {

    @EmbeddedId
    private ProviderProviderDataColumnMappingId id;

    @Column(name = "data_colm_expn", nullable = false, length = 500)
    @Comment("데이터컬럼설명")
    private String dataColumnExpn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prvd_no", referencedColumnName = "prvd_no", foreignKey = @ForeignKey(name = "TMD_PRVDDATACOLML_FK01"), nullable = false)
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

    public void softDelete() {
        super.softDelete();
    }
}
