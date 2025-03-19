package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.bbs.FaqCreateReq;
import com.sk.domain.admin.dto.request.bbs.FaqUpdateReq;
import com.sk.domain.admin.dto.request.bbs.NoticeCreateReq;
import com.sk.domain.admin.dto.request.bbs.NoticeUpdateReq;
import com.sk.domain.shared.entity.base.BaseEntity;

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

    @Column(name = "bbm_type_cd", nullable = false, length = 10)
    @Comment("게시글유형코드")
    private String bulletinTypeCode;

    @Column(name = "ttop_fix_yn", length = 1, columnDefinition = "char(1)")
    @Comment("상단고정여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private boolean isTopFixed;

    @Column(name = "popup_use_yn", length = 1, columnDefinition = "char(1)")
    @Comment("팝업사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private boolean isPopupUse;

    @Column(name = "popup_post_bgn_dt", length = 8)
    @Comment("팝업게시시작일자")
    private String popupPostBeginDt;

    @Column(name = "popup_post_end_dt", length = 8)
    @Comment("팝업게시종료일자")
    private String popupPostEndDt;

    @Column(name = "bbm_ctgr_spt_cd", length = 10)
    @Comment("게시글카테고리구분코드")
    private String bulletinCategorySeparateCode;

    @Column(name = "twhle_post_yn", length = 1, columnDefinition = "char(1)")
    @Comment("전체게시여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isWholePost;

    @Column(name = "bbm_vald_bgn_dt", length = 8)
    @Comment("게시글유효시작일자")
    private String bulletinValidBeginDt;

    @Column(name = "bbm_vald_end_dt", length = 8)
    @Comment("게시글유효종료일자")
    private String bulletinValidEndDt;

    @Column(name = "vald_perd_urstr_yn", length = 1, columnDefinition = "char(1)")
    @Comment("유효기간미제한여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private boolean isValidPerioUnrestrict;

    @Column(name = "bbm_titl_nm", length = 255)
    @Comment("게시글제목명")
    private String bulletinTitleName;

    @Column(name = "bbm_cntt", columnDefinition = "text")
    @Comment("게시글내용")
    private String bulletinContent;

    @Builder
    public BulletinBoard(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Integer bulletinNo,
            String bulletinTypeCode,
            boolean isTopFixed,
            boolean isPopupUse,
            String popupPostBeginDt,
            String popupPostEndDt,
            String bulletinCategorySeparateCode,
            boolean isWholePost,
            String bulletinValidBeginDt,
            String bulletinValidEndDt,
            boolean isValidPerioUnrestrict,
            String bulletinTitleName,
            String bulletinContent) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);

        this.bulletinNo = bulletinNo;
        this.bulletinTypeCode = bulletinTypeCode;
        this.isTopFixed = isTopFixed;
        this.isPopupUse = isPopupUse;
        this.popupPostBeginDt = popupPostBeginDt;
        this.popupPostEndDt = popupPostEndDt;
        this.bulletinCategorySeparateCode = bulletinCategorySeparateCode;
        this.isWholePost = isWholePost;
        this.bulletinValidBeginDt = bulletinValidBeginDt;
        this.bulletinValidEndDt = bulletinValidEndDt;
        this.isValidPerioUnrestrict = isValidPerioUnrestrict;
        this.bulletinTitleName = bulletinTitleName;
        this.bulletinContent = bulletinContent;
    }

    public static BulletinBoard of(NoticeCreateReq dto) {
        return BulletinBoard.builder()
                .bulletinTypeCode("NOTICE")
                .isTopFixed(dto.isTopFixed())
                .isPopupUse(dto.isPopupUse())
                .popupPostBeginDt(DateUtil.toYmd(dto.popupPostBeginDt()))
                .popupPostEndDt(DateUtil.toYmd(dto.popupPostEndDt()))
                .bulletinCategorySeparateCode(dto.bulletinCategorySeparateCode())
                .isWholePost(dto.isWholePost())
                .bulletinValidBeginDt(DateUtil.toYmd(dto.bulletinValidBeginDt()))
                .bulletinValidEndDt(DateUtil.toYmd(dto.bulletinValidEndDt()))
                .bulletinTitleName(dto.bulletinTitleName())
                .bulletinContent(dto.bulletinContent())
                .isValidPerioUnrestrict(false) // do not used
                .build();
    }

    public void update(NoticeUpdateReq dto) {

        if (ObjectUtils.isNotEmpty(dto.isTopFixed())) {
            this.isTopFixed = dto.isTopFixed();
        }

        if (ObjectUtils.isNotEmpty(dto.isPopupUse())) {
            this.isPopupUse = dto.isPopupUse();
        }

        if (ObjectUtils.isNotEmpty(dto.popupPostBeginDt())) {
            this.popupPostBeginDt = DateUtil.toYmd(dto.popupPostBeginDt());
        }

        if (ObjectUtils.isNotEmpty(dto.popupPostEndDt())) {
            this.popupPostEndDt = DateUtil.toYmd(dto.popupPostEndDt());
        }

        if (StringUtils.isNotEmpty(dto.bulletinCategorySeparateCode())) {
            this.bulletinCategorySeparateCode = dto.bulletinCategorySeparateCode();
        }

        if (ObjectUtils.isNotEmpty(dto.popupPostEndDt())) {
            this.popupPostEndDt = DateUtil.toYmd(dto.popupPostEndDt());
        }

        if (ObjectUtils.isNotEmpty(dto.isWholePost())) {
            this.isWholePost = dto.isWholePost();
        }

        if (ObjectUtils.isNotEmpty(dto.bulletinValidBeginDt())) {
            this.bulletinValidBeginDt = DateUtil.toYmd(dto.bulletinValidBeginDt());
        }

        if (ObjectUtils.isNotEmpty(dto.bulletinValidEndDt())) {
            this.bulletinValidEndDt = DateUtil.toYmd(dto.bulletinValidEndDt());
        }

        if (StringUtils.isNotEmpty(dto.bulletinTitleName())) {
            this.bulletinTitleName = dto.bulletinTitleName();
        }

        if (StringUtils.isNotEmpty(dto.bulletinContent())) {
            this.bulletinContent = dto.bulletinContent();
        }
    }

    public static BulletinBoard of(FaqCreateReq dto) {
        return BulletinBoard.builder()
                .bulletinTypeCode("FAQ")
                .bulletinCategorySeparateCode(dto.bulletinCategorySeparateCode())
                .bulletinTitleName(dto.bulletinTitleName())
                .bulletinContent(dto.bulletinContent())
                .isTopFixed(false) // do not used
                .isPopupUse(false) // do not used
                .popupPostBeginDt("99991231") // do not used
                .popupPostEndDt("99991231") // do not used
                .isWholePost(false) // do not used
                .bulletinValidBeginDt("99991231") // do not used
                .bulletinValidEndDt("99991231") // do not used
                .isValidPerioUnrestrict(false) // do not used
                .build();
    }

    public void update(FaqUpdateReq dto) {

        if (StringUtils.isNotEmpty(dto.bulletinCategorySeparateCode())) {
            this.bulletinCategorySeparateCode = dto.bulletinCategorySeparateCode();
        }

        if (StringUtils.isNotEmpty(dto.bulletinTitleName())) {
            this.bulletinTitleName = dto.bulletinTitleName();
        }

        if (StringUtils.isNotEmpty(dto.bulletinContent())) {
            this.bulletinContent = dto.bulletinContent();
        }
    }

    public void softDelete() {
        super.softDelete();
    }
}