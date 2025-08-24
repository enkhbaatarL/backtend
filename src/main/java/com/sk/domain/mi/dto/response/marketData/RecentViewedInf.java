package com.sk.domain.mi.dto.response.marketData;

import java.time.LocalDateTime;

public interface RecentViewedInf {
  int getPrvdNo();
  String getPrvdNm();
  int getGdsNo();
  int getGdsDataNo();
  String getGdsDataNm();
  String getGdsDataExpn();
  LocalDateTime getMaxInqr();
}
