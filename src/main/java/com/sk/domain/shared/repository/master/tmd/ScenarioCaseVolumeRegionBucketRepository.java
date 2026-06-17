package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ScenarioCaseVolumeRegionBucket;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseVolumeRegionBucketRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioCaseVolumeRegionBucketRepository extends JpaRepository<ScenarioCaseVolumeRegionBucket, Long> {

    @Query("""
        SELECT new com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseVolumeRegionBucketRow(
            b.dataTypeCode,
            b.regionCode,
            b.jsonSchemaVersion,
            b.volumeDataJson
        )
          FROM ScenarioCaseVolumeRegionBucket b
         WHERE b.isDeleted = false
           AND b.runSeq = :runSeq
           AND b.segmentCode = :segmentCode
           AND b.periodUnitSeparateCode = :periodUnitSeparateCode
           AND b.dataTypeCode IN :dataTypeCodes
           AND b.regionCode IN :regionCodes
         ORDER BY b.dataTypeCode,
                  b.regionCode
        """)
    List<ScenarioCaseVolumeRegionBucketRow> findRowsByRunSeqAndFilters(
        @Param("runSeq") Long runSeq,
        @Param("segmentCode") String segmentCode,
        @Param("periodUnitSeparateCode") String periodUnitSeparateCode,
        @Param("dataTypeCodes") List<String> dataTypeCodes,
        @Param("regionCodes") List<String> regionCodes
    );
}
