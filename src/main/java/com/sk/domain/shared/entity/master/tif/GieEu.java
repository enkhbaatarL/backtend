package com.sk.domain.shared.entity.master.tif;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.admin.dto.internal.UserInfoWithCompany;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TIF_Gas Infrastructure EU총합 수신")
@Table(name = "tif_gier_eu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GieEu extends BaseEntity {

    public static final String GIE_EU_TABLE_NAME = "tif_gier_eu";
    public static final String GIE_EU_CODE_CURRENT = "CURRENT";
    public static final String GIE_EU_CODE_1Y_AGO = "1Y_AGO";
    public static final String GIE_EU_CODE_2Y_AGO = "2Y_AGO";
    public static final String GIE_EU_CODE_3Y_AGO = "3Y_AGO";
    public static final String GIE_EU_CODE_PAST_5Y_AVG = "PAST_5Y_AVG";
    public static final String GIE_EU_CODE_PAST_5Y_MIN = "PAST_5Y_MIN";
    public static final String GIE_EU_CODE_PAST_5Y_MAX = "PAST_5Y_MAX";


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_reg_sn", nullable = false)
    @Comment("데이터등록일련번호")
    private Integer dataRegistrationSn;

    @Column(name = "code", nullable = false, length = 20)
    @Comment("EU가스데이터분류")
    private String code;

    @Column(name = "date", nullable = false)
    @Comment("원천전달데이터일시")
    private LocalDateTime date;

    @Column(name = "gas_in_storage", precision = 15, scale = 4)
    @Comment("저장가스총량")
    @Setter
    private BigDecimal gasInStorage;

    @Column(name = "gas_in_storage_mt", precision = 15, scale = 4)
    @Comment("저장가스총량-백만톤")
    @Setter
    private BigDecimal gasInStorageMt;

    @Column(name = "full", precision = 15, scale = 4)
    @Comment("적재율")
    @Setter
    private BigDecimal full;


    @Builder
    public GieEu(Integer dataRegistrationSn, String code, LocalDateTime date, BigDecimal gasInStorage,
        BigDecimal gasInStorageMt, BigDecimal full) {
        this.dataRegistrationSn = dataRegistrationSn;
        this.code = code;
        this.date = date;
        this.gasInStorage = gasInStorage;
        this.gasInStorageMt = gasInStorageMt;
        this.full = full;
    }

    public static GieEu of(String code, LocalDateTime date, BigDecimal gasInStorage, BigDecimal gasInStorageMt, BigDecimal full) {
        return GieEu.builder()
            .code(code)
            .date(date)
            .gasInStorage(gasInStorage)
            .gasInStorageMt(gasInStorageMt)
            .full(full)
            .build();
    }
}
