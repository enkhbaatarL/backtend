package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.bbs.BulletinBoardCreateReq;
import com.sk.domain.admin.dto.request.company.CompanyReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tcm_bbsm", indexes = {
        @Index(name = "TCM_BBSM_IX01", columnList = "cust_cpny_no")
})
@Comment("TCM_게시판기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BulletinBoard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bbm_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("게시글번호")
    private Integer bulletinNo;

    @Column(name = "bbm_titl_nm", nullable = false, length = 255)
    @Comment("게시글제목명")
    private String bulletinTitleName;

    @Column(name = "bbm_type_cd", nullable = false, length = 10)
    @Comment("게시글유형코드 [NOTICE, FAQ]")
    private String bulletinTypeCode;

    @Column(name = "twhle_post_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("전체게시여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isWholePost;

    @Column(name = "bbm_cntt", columnDefinition = "text")
    @Comment("게시글내용")
    private String bulletinContent;

    @Column(name = "bbm_ctgr_spt_cd", nullable = false, length = 10)
    @Comment("게시글카테고리구분코드")
    private String bulletinCategorySeparateCode;

    @Column(name = "ttop_fix_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("상단고정여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private boolean isTopFixed;

    @Column(name = "popup_use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("팝업사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private boolean isPopupUse;

    @Column(name = "bbm_vald_bgn_dt", length = 8)
    @Comment("게시글유효시작일자")
    private String bulletinValidBeginDt;

    @Column(name = "bbm_vald_end_dt", length = 8)
    @Comment("게시글유효종료일자")
    private String bulletinValidEndDt;

    @Column(name = "vald_perd_urstr_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("유효기간미제한여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private boolean isValidPerioUnrestrict;

    @Builder
    public BulletinBoard(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer bulletinNo, String bulletinTypeCode, boolean isWholePost,
            String bulletinCategorySeparateCode, String bulletinTitleName, String bulletinContent, boolean isTopFixed,
            boolean isPopupUse, String bulletinValidBeginDt, String bulletinValidEndDt, boolean isValidPerioUnrestrict) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.bulletinNo = bulletinNo;
        this.bulletinTypeCode = bulletinTypeCode;
        this.isWholePost = isWholePost;
        this.bulletinCategorySeparateCode = bulletinCategorySeparateCode;
        this.bulletinTitleName = bulletinTitleName;
        this.bulletinContent = bulletinContent;
        this.isTopFixed = isTopFixed;
        this.isPopupUse = isPopupUse;
        this.bulletinValidBeginDt = bulletinValidBeginDt;
        this.bulletinValidEndDt = bulletinValidEndDt;
        this.isValidPerioUnrestrict = isValidPerioUnrestrict;
    }

    // public static BulletinBoard of(NoticeCreateReq dto) {
    // return BulletinBoard.builder()
    // .customerCompanyCd(dto.companyCd())
    // .customerCompanyNm(dto.companyNm())
    // .useBeginDt(DateUtil.toYmd(dto.usageBeginDate()))
    // .useEndDt(DateUtil.toYmd(dto.usageEndDate()))
    // .build();
    // }

    public void softDelete() {
        super.softDelete();
    }
}