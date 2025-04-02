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
import jakarta.persistence.ForeignKey;
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

    @Column(name = "cd_nm", nullable = false, length = 100)
    @Comment("코드명")
    private String codeNm;

    @Column(name = "cd_rnk", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("코드순위")
    private Short codeRnk;

    @Column(name = "cd_expn", length = 500)
    @Comment("코드설명")
    private String codeExpn;

    @Column(name = "use_bgn_dt", nullable = false, length = 8)
    @Comment("사용시작일자")
    private String useBeginDt;

    @Column(name = "use_end_dt", nullable = false, length = 8)
    @Comment("사용종료일자")
    private String useEndDt;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    @Comment("사용여부")
    private Boolean isUsed;

    @Column(name = "high_cd_grp_id", length = 50)
    @Comment("상위코드그룹ID")
    private String highCodeGroupId;

    @Column(name = "high_cd_id", length = 30)
    @Comment("상위코드ID")
    private String highCodeId;

    @MapsId("codeGroupId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cd_grp_id", nullable = false, referencedColumnName = "cd_grp_id")
    @Comment("공통코드")
    private CommonCode commonCd;

    @Builder
    public CommonCodeDetail(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, CommonCodeDetailId id, CommonCode commonCd, String codeNm,
            Short codeRnk, String codeExpn, String useBeginDt, String useEndDt,
            Boolean isUsed) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.commonCd = commonCd;
        this.codeNm = codeNm;
        this.codeRnk = codeRnk;
        this.codeExpn = codeExpn;
        this.useBeginDt = useBeginDt;
        this.useEndDt = useEndDt;
        this.isUsed = isUsed;
    }
}