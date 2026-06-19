package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.response.nextgen.CmeBenchmarkGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.service.nextgen.support.ScenarioGroupGraphExcelMapper;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * [Dedicated] 시나리오 그룹 그래프 엑셀 다운로드 (MI Base + 시나리오 + CME 합본).
 *
 * MI Base/시나리오 시리즈는 midb(JPA)에서, CME 기준선은 전용 DB {@code midb-skgas}(MyBatis)에서 조회한다.
 */
@Service
@Profile({"dedicated", "testdb"})
@RequiredArgsConstructor
public class ScenarioGroupGraphExportDedicatedService {

    private final ScenarioGroupServiceImpl scenarioGroupService;
    private final CmeBenchmarkService cmeBenchmarkService;

    /**
     * 인덱스(JKM/TTF/HH) 그래프를 인덱스별 시트로 묶어 엑셀로 내보낸다.
     * - midb 그래프(시나리오 + MI Base)를 먼저 빌드한 뒤, 인덱스별 CME 기준선을 합친다(트랜잭션 종료 후 전용 DB 라우팅).
     * - MI와 CME는 기준이 달라 기간 범위가 어긋날 수 있으며, x축은 양측 union 없는 구간은 빈칸으로 둔다.
     */
    public ByteArrayInputStream exportScenarioGroupGraph(Long scenarioGroupId, GraphPeriodUnit periodUnit) {
        Map<IndexName, ScenarioGroupGraphRes> graphs = scenarioGroupService.buildGraphs(scenarioGroupId, periodUnit);

        List<ScenarioGroupGraphRes> merged = graphs.entrySet().stream()
            .map(entry -> mergeCmeBenchmark(entry.getKey(), entry.getValue(), periodUnit))
            .toList();

        return ScenarioGroupGraphExcelMapper.toExcel(merged);
    }

    /** 인덱스별 CME 기준선을 같은 그래프에 기준선 시리즈로 합친다. */
    private ScenarioGroupGraphRes mergeCmeBenchmark(IndexName indexName, ScenarioGroupGraphRes graph, GraphPeriodUnit periodUnit) {
        CmeBenchmarkGraphRes cme = cmeBenchmarkService.getCmeBenchmarkGraph(indexName, periodUnit);
        return ScenarioGroupGraphRes.withBenchmark(graph, cme.dates(), cme.baseSeries());
    }
}
