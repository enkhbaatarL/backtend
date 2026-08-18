package com.sk.domain.mi.service.nextgen.support;

import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 시나리오 재분배 확률 계산기.
 *
 * <p>그룹 내 '폐기' 시나리오를 제외한 나머지가 원본 probability_score 비례로 100을 나눠 갖는다.
 * 정수 배분의 합 100 보장은 최대잔여법(largest remainder)으로 처리하고, 잔여 배분 동점은 입력 순서(displayOrder)를 따른다.
 *
 * <p>조회 시점 계산 전용 — DB의 probability_score는 변경하지 않는다.
 */
public final class ScenarioProbabilityAllocator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ScenarioProbabilityAllocator() {
    }

    /** 재분배 입력 항목. */
    public record Item(Long scenarioId, BigDecimal probabilityScore, boolean discarded) {

        public static Item from(ScenarioBaseRow row) {
            boolean discarded = ScenarioDisplayStatus.from(row.status()) == ScenarioDisplayStatus.DISCARDED;
            return new Item(row.scenarioId(), row.probabilityScore(), discarded);
        }
    }

    /** 그룹 단위 시나리오 행 목록 → scenarioId 별 재분배 확률. */
    public static Map<Long, Integer> redistributeRows(List<ScenarioBaseRow> rows) {
        return redistribute(rows.stream().map(Item::from).toList());
    }

    /**
     * 그룹 생성 API 버전(tmm_mi_scenario_group_m.api_version)으로 확률 스케일을 판정한다.
     * v4만 0~1 비율 스케일이고 v1과 v3은 0~100 정수다 (2026-08-18 실DB 전수 실측: v1 그룹 합 100.00,
     * v3 확률 38~86, v4 전수 0.10~0.34 합 1.00. v2 그룹은 미존재).
     * dimension_definitions 파싱 성공 여부로 추론하던 기존 방식은 정의 JSON이 깨지면
     * 확률이 조용히 0으로 붕괴되는 경로가 있어 명시 컬럼 판정으로 교체했다.
     */
    public static boolean ratioScale(String apiVersion) {
        return "v4".equals(apiVersion);
    }

    /**
     * 원본 probability_score를 표시용 정수 퍼센트로 변환한다.
     * v1/v3은 0~100 스케일이라 정수부 그대로, v4는 0~1 스케일이라 100을 곱해 반올림한다.
     * ratioScale 판정은 그룹 api_version 기준(ratioScale(String)) — definitions 파싱 추론은 폐기.
     * 재분배(redistribute)는 비례 계산이라 스케일과 무관하며, 이 변환은 원본 확률을 직접 노출하는 응답에만 쓴다.
     */
    public static Integer toDisplayPercent(BigDecimal probabilityScore, boolean ratioScale) {
        if (probabilityScore == null) {
            return null;
        }
        if (ratioScale) {
            return probabilityScore.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).intValue();
        }
        return probabilityScore.intValue();
    }

    /**
     * 표시용 정수 퍼센트를 엔진 저장 스케일로 되돌린다 (toDisplayPercent의 역변환, 확정 요청 프록시용).
     * v4(definitions 기반)는 100으로 나눠 0~1 비율로, v1은 정수 그대로 보낸다.
     * v4 확정이 0~1 그대로 저장하는 것은 2026-08-14 그룹 184 실통으로 확정된 사실이다.
     */
    public static BigDecimal toEngineScale(Integer displayPercent, boolean ratioScale) {
        if (displayPercent == null) {
            return null;
        }
        BigDecimal value = BigDecimal.valueOf(displayPercent);
        return ratioScale ? value.movePointLeft(2) : value;
    }

    /**
     * scenarioId → 재분배 확률(정수). 활성(비폐기) 시나리오 합 100, 폐기 시나리오는 0.
     * 활성이 없으면 전부 0, 활성 가중치 합이 0이면 균등 분배한다.
     */
    public static Map<Long, Integer> redistribute(List<Item> items) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        items.forEach(item -> result.put(item.scenarioId(), 0));

        List<Item> active = items.stream().filter(item -> !item.discarded()).toList();
        if (active.isEmpty()) {
            return result;
        }

        BigDecimal total = active.stream()
            .map(ScenarioProbabilityAllocator::weight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.signum() == 0) {
            int base = 100 / active.size();
            int remainder = 100 % active.size();
            for (int i = 0; i < active.size(); i++) {
                result.put(active.get(i).scenarioId(), base + (i < remainder ? 1 : 0));
            }
            return result;
        }

        record Share(Long scenarioId, int floor, BigDecimal fraction) {
        }
        List<Share> shares = new ArrayList<>(active.size());
        int floorSum = 0;
        for (Item item : active) {
            BigDecimal exact = weight(item).multiply(HUNDRED).divide(total, 6, RoundingMode.DOWN);
            int floor = exact.intValue();
            shares.add(new Share(item.scenarioId(), floor, exact.subtract(BigDecimal.valueOf(floor))));
            floorSum += floor;
        }

        shares.forEach(share -> result.put(share.scenarioId(), share.floor()));
        shares.stream()
            .sorted(Comparator.comparing(Share::fraction).reversed()) // stable sort → 동점은 입력 순서
            .limit(100L - floorSum)
            .forEach(share -> result.merge(share.scenarioId(), 1, Integer::sum));
        return result;
    }

    private static BigDecimal weight(Item item) {
        return item.probabilityScore() == null ? BigDecimal.ZERO : item.probabilityScore();
    }

}
