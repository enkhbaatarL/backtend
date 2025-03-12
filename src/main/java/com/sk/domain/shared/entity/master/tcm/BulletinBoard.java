package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    @Comment("게시글유형코드")
    private String bulletinTypeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", referencedColumnName = "cust_cpny_no")
    @Comment("고객회사")
    private CustomerCompany customerCompany;

    @Column(name = "bbm_cntt", columnDefinition = "text")
    @Comment("게시글내용")
    private String bulletinContent;

    @Builder
    public BulletinBoard(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer bulletinNo, String bulletinTitleName, String bulletinTypeCode,
            CustomerCompany customerCompany, String bulletinContent) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.bulletinNo = bulletinNo;
        this.bulletinTitleName = bulletinTitleName;
        this.bulletinTypeCode = bulletinTypeCode;
        this.customerCompany = customerCompany;
        this.bulletinContent = bulletinContent;
    }

}