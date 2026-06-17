package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Comment("TMD_시나리오케이스실행물량지역내역")
@Table(
    name = "tmd_mdl_scnrcasevolrgnl",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "tmd_mdl_scnrcasevolrgnl_ux01",
            columnNames = {"run_seq", "sgmt_cd", "data_type_cd", "rgn_cd", "perd_unit_spt_cd"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioCaseVolumeRegionBucket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vol_rgn_seq", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("물량지역순번")
    private Long volumeRegionSeq;

    @Column(name = "run_seq", nullable = false, columnDefinition = "int unsigned")
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Comment("실행순번")
    private Long runSeq;

    @Column(name = "sgmt_cd", nullable = false, length = 20)
    @Comment("세그먼트코드")
    private String segmentCode;

    @Column(name = "data_type_cd", nullable = false, length = 30)
    @Comment("데이터타입코드")
    private String dataTypeCode;

    @Column(name = "rgn_cd", nullable = false, length = 50)
    @Comment("지역코드")
    private String regionCode;

    @Column(name = "perd_unit_spt_cd", nullable = false, length = 2)
    @Comment("기간단위구분코드")
    private String periodUnitSeparateCode;

    @Column(name = "json_schm_ver", nullable = false, columnDefinition = "smallint unsigned")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Comment("JSON스키마버전")
    private Integer jsonSchemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vol_data_json", nullable = false, columnDefinition = "json")
    @Comment("물량데이터JSON")
    private String volumeDataJson;

    public ScenarioCaseVolumeRegionBucket(
        LocalDateTime createDt,
        LocalDateTime modifyDt,
        String createUserId,
        String modifyUserId,
        Boolean isDeleted,
        Long volumeRegionSeq,
        Long runSeq,
        String segmentCode,
        String dataTypeCode,
        String regionCode,
        String periodUnitSeparateCode,
        Integer jsonSchemaVersion,
        String volumeDataJson
    ) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.volumeRegionSeq = volumeRegionSeq;
        this.runSeq = runSeq;
        this.segmentCode = segmentCode;
        this.dataTypeCode = dataTypeCode;
        this.regionCode = regionCode;
        this.periodUnitSeparateCode = periodUnitSeparateCode;
        this.jsonSchemaVersion = jsonSchemaVersion;
        this.volumeDataJson = volumeDataJson;
    }
}
