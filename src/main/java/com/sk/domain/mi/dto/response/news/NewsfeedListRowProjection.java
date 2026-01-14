package com.sk.domain.mi.dto.response.news;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface NewsfeedListRowProjection {

    Long getNewsSignificanceId();

    Long getDataRegistrationSerialNo();

    LocalDateTime getFirstCreated();

    String getNewsMarketCategoryCode();

    BigDecimal getNewsSignificanceScore();

    Long getNewsSignalId();

    BigDecimal getSignificantNewsScore();

    Long getNewsSummaryId();

    String getSimilarityGroupStatusCode();

    String getBriefSummary();

    String getSummary();

    String getProvider();

    String getTitle();

    String getRegions();
}
