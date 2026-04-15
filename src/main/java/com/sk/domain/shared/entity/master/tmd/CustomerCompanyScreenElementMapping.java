package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "tmd_custcpnyscrnelmntmappl")
@Comment("TMD_고객회사화면요소매핑내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerCompanyScreenElementMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cust_cpny_scrn_elmnt_map_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("고객회사화면요소매핑번호")
    private Integer customerCompanyScreenElementMapNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", nullable = false)
    @Comment("고객회사번호")
    private CustomerCompany customerCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrn_elmnt_no", nullable = false)
    @Comment("화면요소번호")
    private ScreenElement screenElement;

    @Column(name = "vsblty_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("노출여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isExposed;

    @Column(name = "ord_no", columnDefinition = "smallint unsigned")
    @Comment("정렬순서")
    private Integer orderNo;

    @Builder
    public CustomerCompanyScreenElementMapping(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted, Integer customerCompanyScreenElementMapNo, CustomerCompany customerCompany, ScreenElement screenElement, Boolean isExposed, Integer orderNo) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.customerCompanyScreenElementMapNo = customerCompanyScreenElementMapNo;
        this.customerCompany = customerCompany;
        this.screenElement = screenElement;
        this.isExposed = isExposed;
        this.orderNo = orderNo;
    }
}
