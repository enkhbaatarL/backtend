package com.sk.domain.shared.entity.compositeKey.tcm;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class BulletinBoardCustomerCompanyMappingId {

  @Column(name = "bbm_no", nullable = false, columnDefinition = "int unsigned")
  @Comment("게시글번호")
  private Integer bbmNo;

  @Column(name = "cust_cpny_no", nullable = false, columnDefinition = "mediumint unsigned")
  @Comment("고객회사번호")
  private Integer customerCompanyNo;

  @Builder
  public BulletinBoardCustomerCompanyMappingId(Integer bbmNo, Integer customerCompanyNo) {
    this.bbmNo = bbmNo;
    this.customerCompanyNo = customerCompanyNo;
  }

  public static BulletinBoardCustomerCompanyMappingId of(Integer bbmNo, Integer customerCompanyNo) {
    return BulletinBoardCustomerCompanyMappingId.builder()
        .bbmNo(bbmNo)
        .customerCompanyNo(customerCompanyNo)
        .build();
  }

}
