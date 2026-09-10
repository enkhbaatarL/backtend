package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelSegment;
import java.util.List;

public record VolumesSegmentFilterOptions(
    FundamentalModelSegment segment,
    List<FilterOption> types,
    List<RegionFilterOption> regions,
    List<FilterOption> granularities,
    FundamentalModelPeriodFilterOptions periods
) {
}
