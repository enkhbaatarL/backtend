package com.sk.domain.mi.service.nextgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationStore;
import com.sk.domain.shared.client.ScenarioDesignAgentApi;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScenarioExecutionCancellationTest {

    @Mock private MiScenarioGroupRepository groupRepository;
    @Mock private MiScenarioGroupEventRepository groupEventRepository;
    @Mock private ScenarioDesignAgentApi scenarioDesignAgentApi;
    @Mock private ScenarioCaseRepository caseRepository;
    @Mock private ScenarioCaseRunHistoryRepository historyRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private PriceInterpretationStore priceInterpretationStore;

    @InjectMocks private ScenarioGroupServiceImpl scenarioGroupService;

    @Test
    void flushesManagedLifecycleChangesBeforeCancellingRunningRowsAndSoftDeletingAllRows() {
        MiScenarioGroup group = MiScenarioGroup.builder()
            .scenarioGroupId(1L).status(ScenarioGroupStatus.CREATED).build();
        MiScenario target = MiScenario.builder()
            .scenarioId(11L).scenarioGroup(group).status(ScenarioStatus.CREATED).build();
        given(groupRepository.findByScenarioGroupIdAndIsDeletedFalse(1L)).willReturn(Optional.of(group));
        given(groupRepository.findScenarioEntitiesByGroupIdForUpdate(1L)).willReturn(List.of(target));
        given(authorizationService.getSubject()).willReturn("tester");
        given(caseRepository.findCaseIdsByScenarioIds(List.of(11L))).willReturn(List.of(101L));

        scenarioGroupService.deleteScenarioGroup(1L);

        assertThat(group.getIsDeleted()).isTrue();
        assertThat(target.getIsDeleted()).isTrue();
        InOrder order = inOrder(groupRepository, historyRepository, caseRepository);
        order.verify(groupRepository).flush();
        order.verify(caseRepository).findCaseIdsByScenarioIds(List.of(11L));
        order.verify(historyRepository).cancelRunningByCaseIds(
            eq(List.of(101L)), eq(FundamentalModelRunStatus.FAILED), eq(FundamentalModelRunStatus.RUNNING),
            any(LocalDateTime.class), eq("SCENARIO_CANCELLED"), any(LocalDateTime.class), eq("tester"));
        order.verify(caseRepository).cancelRunningByScenarioIds(
            eq(List.of(11L)), eq(FundamentalModelCaseStatus.FAILED), eq(FundamentalModelCaseStatus.RUNNING),
            any(LocalDateTime.class), eq("tester"));
        order.verify(caseRepository).softDeleteByScenarioIds(
            eq(List.of(11L)), any(LocalDateTime.class), eq("tester"));
        order.verify(historyRepository).softDeleteByCaseIds(
            eq(List.of(101L)), any(LocalDateTime.class), eq("tester"));
    }
}
