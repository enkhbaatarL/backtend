package com.sk.domain.shared.enums;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// @formatter:off
@Getter
@RequiredArgsConstructor
public enum NoticeCategory {

    CATEGORY1("CATEGORY1", "카테고리1"), 
    CATEGORY2("CATEGORY2", "카테고리2"),
    CATEGORY3("CATEGORY3", "카테고리3");

  private final String code;
  private final String name;

  /**
     * code 값으로 name을 찾아 반환한다.
     * @param code 조회할 코드
     * @return 대응되는 name, 없으면 null
     */
    public static String getNameByCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                     .filter(c -> c.code.equals(code))
                     .map(NoticeCategory::getName)
                     .findFirst()
                     .orElse(null);
    }
}
// @formatter:on