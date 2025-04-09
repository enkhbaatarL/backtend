package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.BulletinBoardType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tcm_bbsm")
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
    private String bulletinTypeCd;

    @Column(name = "ttop_fix_yn", length = 1, columnDefinition = "char(1)")
    @Comment("상단고정여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isTopFixed;

    @Column(name = "popup_use_yn", length = 1, columnDefinition = "char(1)")
    @Comment("팝업사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isPopupUse;

    @Column(name = "popup_post_bgn_dt", length = 8)
    @Comment("팝업게시시작일자")
    private String popupPostBeginDt;

    @Column(name = "popup_post_end_dt", length = 8)
    @Comment("팝업게시종료일자")
    private String popupPostEndDt;

    @Column(name = "bbm_ctgr_spt_cd", length = 10)
    @Comment("게시글카테고리구분코드")
    private String bulletinCategorySeparateCd;

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
    private Boolean isValidPerioUnrestrict;

    @Column(name = "bbm_titl_nm", length = 255)
    @Comment("게시글제목명")
    private String bulletinTitleNm;

    @Column(name = "bbm_cntt", columnDefinition = "text")
    @Comment("게시글내용")
    private String bulletinCntt;

    @Builder
    public BulletinBoard(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Integer bulletinNo,
            String bulletinTypeCd,
            Boolean isTopFixed,
            Boolean isPopupUse,
            String popupPostBeginDt,
            String popupPostEndDt,
            String bulletinCategorySeparateCd,
            Boolean isWholePost,
            String bulletinValidBeginDt,
            String bulletinValidEndDt,
            Boolean isValidPerioUnrestrict,
            String bulletinTitleNm,
            String bulletinCntt) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);

        this.bulletinNo = bulletinNo;
        this.bulletinTypeCd = bulletinTypeCd;
        this.isTopFixed = isTopFixed;
        this.isPopupUse = isPopupUse;
        this.popupPostBeginDt = popupPostBeginDt;
        this.popupPostEndDt = popupPostEndDt;
        this.bulletinCategorySeparateCd = bulletinCategorySeparateCd;
        this.isWholePost = isWholePost;
        this.bulletinValidBeginDt = bulletinValidBeginDt;
        this.bulletinValidEndDt = bulletinValidEndDt;
        this.isValidPerioUnrestrict = isValidPerioUnrestrict;
        this.bulletinTitleNm = bulletinTitleNm;
        this.bulletinCntt = bulletinCntt;
    }

    public static BulletinBoard ofNotice(
            Boolean isTopFixed,
            Boolean isPopupUse,
            String popupPostBeginDt,
            String popupPostEndDt,
            String bulletinCategorySeparateCd,
            Boolean isWholePost,
            String bulletinValidBeginDt,
            String bulletinValidEndDt,
            String bulletinTitleNm,
            String bulletinCntt) {
        return BulletinBoard.builder()
                .bulletinTypeCd(BulletinBoardType.NOTICE.getCode())
                .isTopFixed(isTopFixed)
                .isPopupUse(isPopupUse)
                .popupPostBeginDt(popupPostBeginDt)
                .popupPostEndDt(popupPostEndDt)
                .bulletinCategorySeparateCd(bulletinCategorySeparateCd)
                .isWholePost(isWholePost)
                .bulletinValidBeginDt(bulletinValidBeginDt)
                .bulletinValidEndDt(bulletinValidEndDt)
                .bulletinTitleNm(bulletinTitleNm)
                .bulletinCntt(bulletinCntt)
                .isValidPerioUnrestrict(false) // do not used
                .build();
    }

    public void updateNotice(
            Boolean isTopFixed,
            Boolean isPopupUse,
            String popupPostBeginDt,
            String popupPostEndDt,
            String bulletinCategorySeparateCd,
            Boolean isWholePost,
            String bulletinValidBeginDt,
            String bulletinValidEndDt,
            String bulletinTitleNm,
            String bulletinCntt) {

        if (ObjectUtils.isNotEmpty(isTopFixed)) {
            this.isTopFixed = isTopFixed;
        }

        if (ObjectUtils.isNotEmpty(isPopupUse)) {
            this.isPopupUse = isPopupUse;
        }

        if (StringUtils.isNotEmpty(popupPostBeginDt)) {
            this.popupPostBeginDt = popupPostBeginDt;
        }

        if (StringUtils.isNotEmpty(popupPostEndDt)) {
            this.popupPostEndDt = popupPostEndDt;
        }

        if (StringUtils.isNotEmpty(bulletinCategorySeparateCd)) {
            this.bulletinCategorySeparateCd = bulletinCategorySeparateCd;
        }

        if (StringUtils.isNotEmpty(popupPostEndDt)) {
            this.popupPostEndDt = popupPostEndDt;
        }

        if (ObjectUtils.isNotEmpty(isWholePost)) {
            this.isWholePost = isWholePost;
        }

        if (StringUtils.isNotEmpty(bulletinValidBeginDt)) {
            this.bulletinValidBeginDt = bulletinValidBeginDt;
        }

        if (StringUtils.isNotEmpty(bulletinValidEndDt)) {
            this.bulletinValidEndDt = bulletinValidEndDt;
        }

        if (StringUtils.isNotEmpty(bulletinTitleNm)) {
            this.bulletinTitleNm = bulletinTitleNm;
        }

        if (StringUtils.isNotEmpty(bulletinCntt)) {
            this.bulletinCntt = bulletinCntt;
        }
    }

    public static BulletinBoard ofFaq(
            String bulletinCategorySeparateCd,
            String bulletinTitleNm,
            String bulletinCntt) {
        return BulletinBoard.builder()
                .bulletinTypeCd(BulletinBoardType.FAQ.getCode())
                .bulletinCategorySeparateCd(bulletinCategorySeparateCd)
                .bulletinTitleNm(bulletinTitleNm)
                .bulletinCntt(bulletinCntt)
                .isTopFixed(false) // do not used
                .isPopupUse(false) // do not used
                .popupPostBeginDt(DateUtil.endYmd()) // do not used
                .popupPostEndDt(DateUtil.endYmd()) // do not used
                .isWholePost(false) // do not used
                .bulletinValidBeginDt(DateUtil.endYmd()) // do not used
                .bulletinValidEndDt(DateUtil.endYmd()) // do not used
                .isValidPerioUnrestrict(false) // do not used
                .build();
    }

    public void updateFaq(
            String bulletinCategorySeparateCd,
            String bulletinTitleNm,
            String bulletinCntt) {

        if (StringUtils.isNotEmpty(bulletinCategorySeparateCd)) {
            this.bulletinCategorySeparateCd = bulletinCategorySeparateCd;
        }

        if (StringUtils.isNotEmpty(bulletinTitleNm)) {
            this.bulletinTitleNm = bulletinTitleNm;
        }

        if (StringUtils.isNotEmpty(bulletinCntt)) {
            this.bulletinCntt = bulletinCntt;
        }
    }

    // 재게시 처리용
    public void updateCreateDt(LocalDateTime newCreateDt) {
        this.createDt = newCreateDt;
    }

    // 팝업 1개 유지용
    public void updateIsPopupUse(boolean isPopupUse) {
        this.isPopupUse = isPopupUse;
    }

    public void softDelete() {
        super.softDelete();
    }
}