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
     * 원본 probability_score를 표시용 정수 퍼센트로 변환한다 (HALF_UP 반올림).
     * 확률 스케일은 전 세대 0~100 단일 정수다 — v4가 한때 0~1 비율로 적재했으나
     * Agent팀이 v1 정책(0~100)으로 API 계약과 DB 적재를 통일하기로 확정했다 (2026-08-18 회신).
     * 재분배(redistribute)는 비례 계산이라 스케일과 무관하며, 이 변환은 원본 확률을 직접 노출하는 응답에만 쓴다.
     */
    public static Integer toDisplayPercent(BigDecimal probabilityScore) {
        if (probabilityScore == null) {
            return null;
        }
        return probabilityScore.setScale(0, RoundingMode.HALF_UP).intValue();
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
