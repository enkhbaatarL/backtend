package com.sk.domain.mi.service.nextgen.mock;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScenarioGroupMockServiceTest {

    private ScenarioGroupMockService service;

    @BeforeEach
    void setUp() {
        service = new ScenarioGroupMockService(new ObjectMapper().findAndRegisterModules());
        service.init();
    }

    @ParameterizedTest
    @CsvSource({
        "1, true",
        "2, false"
    })
    void resolvesParameterProposalTargetFromBaseEventMapping(Long scenarioGroupId, boolean expected) {
        ScenarioGroupGraphRes graph = service.getScenarioGroupGraph(
            scenarioGroupId,
            IndexName.JKM,
            GraphPeriodUnit.MONTHLY
        );

        assertThat(graph.parameterProposalTarget()).isEqualTo(expected);
    }
}
