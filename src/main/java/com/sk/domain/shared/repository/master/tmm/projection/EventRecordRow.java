package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;

public interface EventRecordRow {
    LocalDateTime getFirstCreated();
    String getRecordType();
    String getTitle();
    String getSummary();
    Integer getGoodsNo();
    Integer getGoodsDataNo();
    String getFileName();
    Long getSourceObjId();
}
