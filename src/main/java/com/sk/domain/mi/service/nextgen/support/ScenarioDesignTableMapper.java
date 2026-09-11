package com.sk.domain.mi.service.nextgen.support;

import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.design.DesignConfirmRow;
import com.sk.domain.mi.dto.response.nextgen.design.DesignConfirmSignpost;
import com.sk.domain.mi.dto.response.nextgen.design.DesignConfirmTable;
import com.sk.domain.mi.dto.response.nextgen.design.DesignEditRow;
import com.sk.domain.mi.dto.response.nextgen.design.DesignEditTable;
import com.sk.domain.mi.dto.response.nextgen.design.DesignScenarioRow;
import com.sk.domain.mi.dto.response.nextgen.design.DesignTable;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;
import com.sk.domain.shared.enums.nextgen.GlobalPressureBand;
import com.sk.domain.shared.enums.nextgen.ScenarioRole;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FE 설계 표 ↔ Agent 계약 표({@link ScenarioDesignTable}: headers + rows.cells) 변환기.
 * 조회(step1) 오버레이는 Agent→FE({@link #toDesignTable}), 검토/Signpost/확정 요청 프록시는 FE→Agent({@link #toAgentTable}).
 */
public final class ScenarioDesignTableMapper {

    private ScenarioDesignTableMapper() {
    }

    /**
     * FE 편집 표 → Agent 표(검토/Signpost 요청 프록시용).
     * dimensionColumns 순서대로 헤더 displayOrder(1-based)를 부여하고, 행의 dimensions를 cells(key→value) 맵으로 펼친다.
     * (신규 분기항목 등 value=null이면 그대로 null로 보내 Agent가 자동생성하도록 한다)
     */
    public static ScenarioDesignTable toAgentTable(DesignEditTable editTable) {
        if (editTable == null) {
            return new ScenarioDesignTable(List.of(), List.of());
        }

        List<ScenarioDimensionColumnRes> cols = editTable.dimensionColumns() == null ? List.of() : editTable.dimensionColumns();
        List<ScenarioDesignTable.Header> headers = new ArrayList<>(cols.size());
        for (int i = 0; i < cols.size(); i++) {
            headers.add(new ScenarioDesignTable.Header(cols.get(i).key(), cols.get(i).label(), i + 1));
        }

        List<ScenarioDesignTable.Row> rows = (editTable.scenarios() == null ? List.<DesignEditRow>of() : editTable.scenarios()).stream()
            .map(r -> {
                Map<String, String> cells = new LinkedHashMap<>();
                if (r.dimensions() != null) {
                    for (ScenarioDimensionRes d : r.dimensions()) {
                        cells.put(d.key(), d.value()); // value=null -> Agent 자동생성
                    }
                }
                return new ScenarioDesignTable.Row(r.scenarioId(), r.displayOrder(), r.scenarioName(), null, cells, null, null, null, null, null, null, null);
            })
            .toList();

        return new ScenarioDesignTable(headers, rows);
    }

    /**
     * FE 확정 표 → Agent 표(확정 요청 프록시용). 검토 변환에 더해 narrative·확률(metrics)·baseCase·Signpost까지 채운다.
     * 확률은 전 세대 0~100 정수 그대로 전송한다 (Agent팀이 v1 정책으로 통일 확정 — 2026-08-18).
     */
    public static ScenarioDesignTable toAgentConfirmTable(DesignConfirmTable table) {
        if (table == null) {
            return new ScenarioDesignTable(List.of(), List.of());
        }

        List<ScenarioDimensionColumnRes> cols = table.dimensionColumns() == null ? List.of() : table.dimensionColumns();
        List<ScenarioDesignTable.Header> headers = new ArrayList<>(cols.size());
        for (int i = 0; i < cols.size(); i++) {
            headers.add(new ScenarioDesignTable.Header(cols.get(i).key(), cols.get(i).label(), i + 1));
        }

        List<ScenarioDesignTable.Row> rows = (table.scenarios() == null ? List.<DesignConfirmRow>of() : table.scenarios()).stream()
            .map(r -> {
                Map<String, String> cells = new LinkedHashMap<>();
                if (r.dimensions() != null) {
                    for (ScenarioDimensionRes d : r.dimensions()) {
                        cells.put(d.key(), d.value());
                    }
                }
                ScenarioDesignTable.Metrics metrics = new ScenarioDesignTable.Metrics(r.probability() == null ? null : BigDecimal.valueOf(r.probability()), null);
                List<ScenarioDesignTable.Signpost> signposts = r.signposts() == null ? List.of() : r.signposts().stream().map(ScenarioDesignTableMapper::toAgentSignpost).toList();
                return new ScenarioDesignTable.Row(r.scenarioId(), r.displayOrder(), r.scenarioName(), r.narrative(), cells, metrics, r.baseCase() ? "Y" : "N", signposts, null, null, null, null);
            })
            .toList();

        return new ScenarioDesignTable(headers, rows);
    }

    private static ScenarioDesignTable.Signpost toAgentSignpost(DesignConfirmSignpost sp) {
        return new ScenarioDesignTable.Signpost(
            sp.signpostId(),
            sp.label(),
            sp.direction() == null ? null : sp.direction().name(), // SUP 단일 값
            sp.rationale(),
            sp.displayOrder(),
            sp.strength() == null ? null : sp.strength().name(), // "STRONG" etc...
            sp.used() ? "Y" : "N");
    }

    /**
     * Agent 표 → step1 DesignTable. headers는 displayOrder 순으로 컬럼/셀을 만든다.
     * 배지(scenarioRole/pressureBand/pressureRationale)는 검토 콜백이 재산출해 보낸 값을 파싱해 채운다 —
     * 없으면(구 캐시, Agent 미전송) null로 두고 조회 측(enrichOverlayTable)이 presentation 표에서 폴백 복원한다.
     */
    public static DesignTable toDesignTable(ScenarioDesignTable agentTable) {
        if (agentTable == null) {
            return DesignTable.of(List.of(), List.of());
        }

        List<ScenarioDesignTable.Header> headers = agentTable.headers() == null
            ? List.of()
            : agentTable.headers().stream()
                .sorted(Comparator.comparing(h -> h.displayOrder() == null ? Integer.MAX_VALUE : h.displayOrder()))
                .toList();

        List<ScenarioDimensionColumnRes> columns = headers.stream()
            .map(h -> new ScenarioDimensionColumnRes(h.key(), h.label(), null, null))
            .toList();

        List<DesignScenarioRow> rows = (agentTable.rows() == null ? List.<ScenarioDesignTable.Row>of() : agentTable.rows()).stream()
            .map(row -> {
                Map<String, String> cells = row.cells() == null ? Map.of() : row.cells();
                List<ScenarioDimensionRes> dimensions = headers.stream()
                    // value 자체가 표시값이라 valueLabel=value
                    .map(h -> new ScenarioDimensionRes(h.key(), h.label(), cells.get(h.key()), cells.get(h.key()), null, null))
                    .toList();
                return DesignScenarioRow.of(row.scenarioId(), row.displayOrder(), row.title(), "Y".equalsIgnoreCase(row.baselineYn()),
                    ScenarioRole.fromNullable(row.scenarioRole()), GlobalPressureBand.fromNullable(row.pressureBand()),
                    row.pressureRationale(), dimensions, null, null, null);
            })
            .toList();

        return DesignTable.of(columns, rows);
    }

}
