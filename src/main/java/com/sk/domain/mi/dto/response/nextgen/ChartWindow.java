package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 가격전망 그래프의 차트 표시 구간 (월별/연도별).
 * CME가 과거 ~ 먼 미래(2030년)까지 데이터를 들고 와 차트가 길어지는 문제로, 화면·엑셀 모두 동일한 구간으로 잘라서 표기한다. CME/MI Base/시나리오 시리즈 구분 없이 같은 창을 적용한다.
 *   - 월별: 현재월 -6개월 ~ MI Base 최종월 (MI Base가 없으면 현재월 +12개월)
 *   - 연도별: 현재년 -1년 ~ 2030년
 * @param start 구간 시작 (포함)
 * @param end   구간 끝 (포함)
 */
public record ChartWindow(
    String start,
    String end
) {

    /** 연도별 차트 표출 종료 연도 */
    private static final int YEARLY_END_YEAR = 2030;

    /**
     * @param periodUnit 기간 단위
     * @param miBaseEnd  MI Base 최종 기간 (연도별에서는 2030 고정)
     */
    public static ChartWindow of(GraphPeriodUnit periodUnit, String miBaseEnd) {
        if (periodUnit == GraphPeriodUnit.YEARLY) {
            String startYear = String.valueOf(LocalDate.now().getYear() - 1);
            return new ChartWindow(startYear, String.valueOf(YEARLY_END_YEAR));
        }
        YearMonth now = YearMonth.now();
        String start = now.minusMonths(6).toString();
        String end = miBaseEnd != null ? miBaseEnd : now.plusMonths(12).toString();
        return new ChartWindow(start, end);
    }

    public boolean contains(String date) {
        return date.compareTo(start) >= 0 && date.compareTo(end) <= 0;
    }

    /**
     * CME 월 단위 조회 시작 경계.
     * 연도별이면 시작연도 1월.
     */
    public String queryFromMonth() {
        return start.length() == 4 ? "%s-01".formatted(start) : start;
    }

}
