package com.sk.domain.shared.entity.master.tcm;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardCustomerCompanyMappingId;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "tcm_bbscpnymappl")
@Comment("TAM_게시판회사매핑내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BulletinBoardCustomerCompanyMapping extends BaseEntity {

  @EmbeddedId
  private BulletinBoardCustomerCompanyMappingId id;

  @MapsId("bulletinNo")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bbm_no", nullable = false, referencedColumnName = "bbm_no")
  @Comment("게시판")
  private BulletinBoard bulletinBoard;

  @MapsId("customerCompanyNo")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cust_cpny_no", nullable = false, referencedColumnName = "cust_cpny_no")
  @Comment("고객회사")
  private CustomerCompany customerCompany;

  @Builder
  public BulletinBoardCustomerCompanyMapping(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
      String modifyUserId, Boolean isDeleted, BulletinBoardCustomerCompanyMappingId id,
      BulletinBoard bulletinBoard, CustomerCompany customerCompany) {
    super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
    this.id = id;
    this.bulletinBoard = bulletinBoard;
    this.customerCompany = customerCompany;
  }

  public static BulletinBoardCustomerCompanyMapping of(BulletinBoard bulletinBoard, CustomerCompany company) {
    return BulletinBoardCustomerCompanyMapping.builder()
        .id(BulletinBoardCustomerCompanyMappingId.of(bulletinBoard.getBulletinNo(), company.getCustomerCompanyNo()))
        .bulletinBoard(bulletinBoard)
        .customerCompany(company)
        .build();
  }
}