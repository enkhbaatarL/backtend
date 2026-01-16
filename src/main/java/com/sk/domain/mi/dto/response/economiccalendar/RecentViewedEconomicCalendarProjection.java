package com.sk.domain.mi.dto.response.economiccalendar;

import java.time.LocalDateTime;

public interface RecentViewedEconomicCalendarProjection {

    String getCode();

    String getEconomicIndicatorName();

    LocalDateTime getInquiryDateTime();

    String getRefinitivSourceOrganization();
}
