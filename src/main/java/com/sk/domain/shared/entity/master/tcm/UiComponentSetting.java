package com.sk.domain.shared.entity.master.tcm;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(
    name = "tcm_uicmpntsetm",
    uniqueConstraints = @UniqueConstraint(
        name = "tcm_uicmpntsetm_ux01",
        columnNames = {"cust_cpny_no", "scrn_cd", "cmpnt_type_cd", "cmpnt_cd", "itm_key"}
    )
)
@Comment("TCM_UI컴포넌트설정")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UiComponentSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ui_cmpnt_set_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("UI컴포넌트설정번호")
    private Integer uiComponentSettingNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", nullable = false)
    @Comment("고객회사번호")
    private CustomerCompany customerCompany;

    @Column(name = "scrn_cd", nullable = false, length = 30)
    @Comment("화면코드")
    private String screenCode;

    @Column(name = "cmpnt_type_cd", nullable = false, length = 20)
    @Comment("컴포넌트타입코드")
    private String componentTypeCode;

    @Column(name = "cmpnt_cd", nullable = false, length = 50)
    @Comment("컴포넌트코드")
    private String componentCode;

    @Column(name = "itm_key", nullable = false, length = 50)
    @Comment("항목키")
    private String itemKey;

    @Column(name = "msg_cd", length = 50)
    @Comment("메시지코드")
    private String messageCode;

    @Column(name = "label_txt", length = 100)
    @Comment("라벨텍스트")
    private String labelText;

    @Column(name = "mark_yn", nullable = false, columnDefinition = "char(1)")
    @Comment("표시여부")
    @ColumnDefault("'Y'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isVisible;

    @Column(name = "ord_no", columnDefinition = "smallint unsigned")
    @Comment("정렬순서")
    private Integer orderNo;

    @Column(name = "set_val", length = 500)
    @Comment("설정값")
    private String settingValue;
}
