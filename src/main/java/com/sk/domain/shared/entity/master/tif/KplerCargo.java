package com.sk.domain.shared.entity.master.tif;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
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
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TIF_Kpler Cargo 데이터 수신")
@Table(name = "tif_kpler_cargo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KplerCargo extends BaseEntity {

    public static final String TABLE_NAME = "tif_kpler_cargo";
    public enum CODE{
        USA_Export,
        USA_Export_mtd;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_reg_sn", nullable = false)
    @Comment("데이터등록일련번호")
    private Integer dataRegistrationSn;

    @Column(name = "code", nullable = false, length = 30)
    @Comment("원천ID코드")
    private String code;

    @Column(name = "date", nullable = false)
    @Comment("원천전달데이터일시")
    @Setter
    private LocalDateTime date;

    @Column(name = "value", precision = 15, scale = 4)
    @Comment("값")
    @Setter
    private BigDecimal value;

    @Builder
    public KplerCargo(Integer dataRegistrationSn, String code, LocalDateTime date, BigDecimal value) {
        this.dataRegistrationSn = dataRegistrationSn;
        this.code = code;
        this.date = date;
        this.value = value;
    }
}
