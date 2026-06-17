package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioConfirmationReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationReq;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStageRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStateRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDimensionTemplateRes;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 시나리오 설계 실 구현 (Agent 프록시). Agent 연동 확정 전이라 대부분 미구현(mock 라우팅)이나,
 * 분기항목/분기기준 템플릿은 master 데이터라 mock 없이 항상 이 실 구현(DB)을 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioDesignServiceImpl implements ScenarioDesignService {

    private static final String NOT_IMPLEMENTED =
        "ScenarioDesign 실 구현 미완성 (Agent 연동 대기). mi.next-gen.mock.design.api.* 를 활성화하세요.";

    private final MiScenarioGroupRepository scenarioGroupRepository;

    @Override
    public ScenarioDesignStageRes startDesign(ScenarioDesignStartReq request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public ScenarioDesignStateRes getDesign(Long scenarioGroupId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    /**
     * 분기항목/분기기준 템플릿 조회 (항상 DB). 그룹의 event_type_l2 기준 template_json을 파싱해 내린다.
     */
    @Override
    public ScenarioDimensionTemplateRes getDimensionTemplates(Long scenarioGroupId) {
        ScenarioGroupDetailBaseRow base = scenarioGroupRepository.findScenarioGroupDetail(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: " + scenarioGroupId));

        List<ScenarioDimensionTemplateRes.Item> items = scenarioGroupRepository
            .findTemplatesByEventTypeL2Ids(List.of(base.eventTypeL2Id())).stream()
            .findFirst()
            .map(row -> ScenarioDimensionMapper.parseTemplateItems(row.templateJson()))
            .orElseGet(List::of);

        return new ScenarioDimensionTemplateRes(items);
    }

    @Override
    public ScenarioDesignStageRes validate(Long scenarioGroupId, ScenarioValidationReq request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public ScenarioDesignStageRes createSignposts(Long scenarioGroupId, ScenarioSignpostsReq request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public ScenarioDesignStageRes confirm(Long scenarioGroupId, ScenarioConfirmationReq request) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public ScenarioDesignStateRes reset(Long scenarioGroupId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

}
