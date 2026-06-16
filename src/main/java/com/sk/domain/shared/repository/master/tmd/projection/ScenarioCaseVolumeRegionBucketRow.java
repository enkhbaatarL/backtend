package com.sk.domain.shared.repository.master.tmd.projection;

public record ScenarioCaseVolumeRegionBucketRow(
    String dataTypeCode,
    String regionCode,
    Integer jsonSchemaVersion,
    String volumeDataJson
) {
}
