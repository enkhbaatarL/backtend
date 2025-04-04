package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import com.sk.domain.shared.listener.ProviderListener;
import jakarta.persistence.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.shared.entity.base.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_프로바이더기본")
@Table(name = "tmd_prvdm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(ProviderListener.class)
public class Provider extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prvd_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("프로바이더번호")
    private Integer providerNo;

    @Column(name = "prvd_nm", nullable = false, length = 100)
    @Comment("프로바이더명")
    private String providerNm;

    @Column(name = "prvd_id", nullable = false, length = 20)
    @Comment("프로바이더ID")
    private String providerId;

    @Column(name = "prvd_expn", length = 500)
    @Comment("프로바이더설명")
    private String providerExpn;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Builder
    public Provider(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer providerNo, String providerNm, String providerId, String providerExpn,
            Boolean isUsed) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.providerId = providerId;
        this.providerExpn = providerExpn;
        this.isUsed = isUsed;
    }

    public static Provider of(ProviderCreateReq dto) {
        return Provider.builder()
                .providerNm(dto.providerNm())
                .providerId(dto.providerId())
                .providerExpn(dto.providerExpn())
                .isUsed(dto.isUsed())
                .build();
    }

    public void update(String providerNm, String providerExpn, Boolean isUsed) {

        if (StringUtils.isNotEmpty(providerNm)) {
            this.providerNm = providerNm;
        }

        if (StringUtils.isNotEmpty(providerExpn)) {
            this.providerExpn = providerExpn;
        }

        if (ObjectUtils.isNotEmpty(isUsed)) {
            this.isUsed = isUsed;
        }
    }

    public void softDelete() {
        super.softDelete();
    }
}
