package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tcm_commcdm")
@Comment("TCM_공통코드기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCode extends BaseEntity {

    @Id
    @Column(name = "cd_grp_id", nullable = false, length = 50)
    @Comment("코드그룹ID")
    private String codeGroupId;

    @Column(name = "cd_grp_nm", nullable = false, length = 100)
    @Comment("코드그룹명")
    private String codeGroupNm;

    @Column(name = "cd_grp_expn", length = 500)
    @Comment("코드그룹설명")
    private String codeGroupExpn;

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

    @Builder
    public CommonCode(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, String codeGroupId, String codeGroupNm, String codeGroupExpn,
            String useBeginDt, String useEndDt, Boolean isUsed) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.codeGroupId = codeGroupId;
        this.codeGroupNm = codeGroupNm;
        this.codeGroupExpn = codeGroupExpn;
        this.useBeginDt = useBeginDt;
        this.useEndDt = useEndDt;
        this.isUsed = isUsed;
    }
}
