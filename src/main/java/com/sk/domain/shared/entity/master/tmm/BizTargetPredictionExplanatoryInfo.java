package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmm.BizTargetPredictionExplanatoryInfoId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tmm_bztgtprdcexppinfom")
@Comment("TMM_비즈타겟예측설명력정보기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BizTargetPredictionExplanatoryInfo extends BaseEntity {
    @EmbeddedId
    private BizTargetPredictionExplanatoryInfoId id;

    @Column(name = "expp_scre", precision = 6, scale = 2)
    @Comment("설명력점수")
    private BigDecimal explanatoryNm;

    @Column(name = "expp_rnk", columnDefinition = "smallint unsigned")
    @Comment("설명력순위")
    private Integer explanatoryRnk;

    @Column(name = "expp_dir_symb_nm", length = 1)
    @Comment("설명력방향기호명")
    private String explanatoryDirectionSymbolNm;

    @Column(name = "prdc_inpt_mnmnc_dtl_cd", length = 50)
    @Comment("예측입력MNEMONIC상세코드")
    private String predictionInputMnemonicDetailCd;
}
