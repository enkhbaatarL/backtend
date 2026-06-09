package com.sk.domain.shared.repository.master.tmm.custom;

import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDimensionTemplateRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioIndividualBaseRow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface CustomMiScenarioGroupRepository {

    Page<ScenarioGroupBaseRow> findScenarioGroupPage(ScenarioGroupListReq request);

    Page<ScenarioIndividualBaseRow> findScenarioPage(ScenarioGroupListReq request);

    Optional<ScenarioGroupDetailBaseRow> findScenarioGroupDetail(Long scenarioGroupId);

    List<ScenarioBaseRow> findScenariosByGroupIds(List<Long> scenarioGroupIds);

    List<ScenarioDimensionTemplateRow> findTemplatesByEventTypeL2Ids(List<Long> eventTypeL2Ids);

}
