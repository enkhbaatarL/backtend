package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;

public interface EventOutlookRow {
    LocalDateTime getPublishedAt();
    String getInstitutionName();
    String getOutlookSummary();
}
