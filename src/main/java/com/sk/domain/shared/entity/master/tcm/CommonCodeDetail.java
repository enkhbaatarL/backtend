package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tcm.CommonCodeDetailId;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "tcm_commcdd")
@Comment("TCM_공통코드상세")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCodeDetail extends BaseEntity {

    @EmbeddedId
    private CommonCodeDetailId id;

    @MapsId("codeGroupId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cd_grp_id", nullable = false, referencedColumnName = "cd_grp_id")
    @Comment("공통코드")
    private CommonCode commonCode;

    @Column(name = "cd_nm", nullable = false, length = 100)
    @Comment("코드명")
    private String codeName;

    @Column(name = "cd_ord_sn", nullable = false, columnDefinition = "tinyint unsigned")
    @Comment("코드순서일련번호")
    private Short codeOrderSeq;

    @Column(name = "cd_expn", length = 500)
    @Comment("코드설명")
    private String codeExpn;

    @Column(name = "use_bgn_dt", nullable = false, length = 8)
    @Comment("사용시작일자")
    private String useBeginDate;

    @Column(name = "use_end_dt", nullable = false, length = 8)
    @Comment("사용종료일자")
    private String useEndDate;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    @Comment("사용여부")
    private Boolean isUsed;

    @Builder
    public CommonCodeDetail(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, CommonCodeDetailId id, CommonCode commonCode, String codeName,
            Short codeOrderSeq, String codeExpn, String useBeginDate, String useEndDate,
            Boolean isUsed) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.commonCode = commonCode;
        this.codeName = codeName;
        this.codeOrderSeq = codeOrderSeq;
        this.codeExpn = codeExpn;
        this.useBeginDate = useBeginDate;
        this.useEndDate = useEndDate;
        this.isUsed = isUsed;
    }
}