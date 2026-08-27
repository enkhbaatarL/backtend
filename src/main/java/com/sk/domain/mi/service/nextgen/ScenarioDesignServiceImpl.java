package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AgentDesignApiException;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioConfirmationReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.mi.dto.response.nextgen.design.*;
import com.sk.domain.mi.service.nextgen.design.EventDesignLockStore;
import com.sk.domain.mi.service.nextgen.design.ScenarioDesignFailureStore;
import com.sk.domain.mi.service.nextgen.design.ScenarioDesignValidationStore;
import com.sk.domain.mi.service.nextgen.support.ScenarioDesignTableMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper.GroupDimensionTemplate;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.client.ScenarioDesignAgentApi;
import com.sk.domain.shared.client.dto.request.ScenarioConfirmationAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateV4AgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioSignpostsAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioValidationAgentReq;
import com.sk.domain.shared.client.dto.response.ScenarioDesignAgentRes;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioDesignServiceImpl implements ScenarioDesignService {

    private final MiScenarioGroupRepository scenarioGroupRepository;
    private final MiScenarioGroupEventRepository scenarioGroupEventRepository;
    private final ScenarioDesignValidationStore validationStore;
    private final ScenarioDesignFailureStore failureStore;
    private final EventDesignLockStore eventDesignLockStore;
    private final ScenarioDesignAgentApi scenarioDesignAgentApi;
    private final AuthorizationService authorizationService;

    /**
     * [변경사항 검토] Agent 바이패스
     *  - 편집된 설계를 Agent 표로 변환해 검토를 요청
     */
    @Override
    public ScenarioDesignStageRes validateDesign(Long scenarioGroupId, ScenarioValidationReq request) {

        requireEditableByCurrentUser(scenarioGroupId);

        Long userId = authorizationService.getUserNo().longValue();
        ScenarioDesignTable table = ScenarioDesignTableMapper.toAgentTable(request.table());

        ScenarioDesignAgentRes res = callAgent(() -> scenarioDesignAgentApi.validate(scenarioGroupId, ScenarioValidationAgentReq.of(userId, table)), "validateDesign");

        return toStageRes(res);
    }

    /**
     * [Signpost 생성] Agent 바이패스
     *  - 검토가 완료된 설계를 Agent 표로 변환해 단계2 진입을 요청 (그룹/시나리오/Signpost 영속화)
     */
    @Override
    public ScenarioDesignStageRes createSignposts(Long scenarioGroupId, ScenarioSignpostsReq request) {

        requireEditableByCurrentUser(scenarioGroupId);

        Long userId = authorizationService.getUserNo().longValue();
        ScenarioDesignTable table = ScenarioDesignTableMapper.toAgentTable(request.table());

        ScenarioDesignAgentRes res = callAgent(() -> scenarioDesignAgentApi.createSignposts(scenarioGroupId, ScenarioSignpostsAgentReq.of(userId, table)), "createSignposts");

        return toStageRes(res);
    }

    /**
     * [최종 확정] Agent 바이패스 — 그룹 편집 + full 확정 표를 Agent에 전달해 확정/스냅샷 저장을 요청한다.
     * <p>확정은 콜백이 없어 이 응답이 최종(status=created/stage=completed)이며, 성공 시 검토 캐시(Redis)를 정리한다.
     * 확정/스냅샷 적재 주체는 Agent다(B.E는 DB에 쓰지 않음).
     */
    @Override
    public ScenarioDesignStageRes confirmDesign(Long scenarioGroupId, ScenarioConfirmationReq request) {

        requireEditableByCurrentUser(scenarioGroupId);

        Long userId = authorizationService.getUserNo().longValue();
        ScenarioConfirmationReq.GroupEdit edit = request.scenarioGroup();
        ScenarioConfirmationAgentReq.GroupEdit groupEdit = edit == null ? null : new ScenarioConfirmationAgentReq.GroupEdit(edit.title(), edit.summary(), edit.description());

        ScenarioDesignTable table = ScenarioDesignTableMapper.toAgentConfirmTable(request.table());

        ScenarioDesignAgentRes res = callAgent(() -> scenarioDesignAgentApi.confirm(scenarioGroupId, ScenarioConfirmationAgentReq.of(userId, groupEdit, table)), "confirmDesign");

        ScenarioDesignStageRes stageRes = toStageRes(res);

        // 확정 완료 → 검토/실패 캐시 정리 (콜백 없는 단계라 여기서 마무리)
        validationStore.delete(scenarioGroupId);
        failureStore.delete(scenarioGroupId);

        return stageRes;
    }

    private ScenarioDesignStageRes toStageRes(ScenarioDesignAgentRes res) {

        if (!res.isSuccess()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "agent response: %s".formatted(JsonUtil.toJson(res.data())));
        }

        ScenarioDesignAgentRes.Data data = res.data();
        return ScenarioDesignStageRes.of(
            data.scenarioGroupId(),
            ScenarioGroupStatus.from(data.status()),
            ScenarioGroupStage.from(data.stage())
        );

    }

    /**
     * Agent 호출을 감싸 통신 예외(호스트 다운/타임아웃/HTTP 에러 status)를 통제된 외부 API 에러로 변환
     */
    private <T> T callAgent(Supplier<T> call, String action) {
        try {
            return call.get();
        } catch (ApiErrorException e) {
            log.error("[Design Agent] {} 응답 오류: status={}, body={}", action, e.getStatusCode(), e.getMessage(), e);
            if (e.getStatusCode() == 409 || e.getStatusCode() == 403) {
                throw AgentDesignApiException.fromAgentBody(e.getStatusCode(), e.getMessage());
            }
            throw AgentDesignApiException.internalFromAgentBody(e.getMessage());
        } catch (ResourceAccessException e) {
            ResultCode.Error code = e.getCause() instanceof SocketTimeoutException ? ResultCode.Error.API_TIMEOUT : ResultCode.Error.API_SERVICE_UNAVAILABLE;
            log.error("[Design Agent] {} 통신 오류: {}", action, e.getMessage(), e);
            throw ApiErrorException.of(code, e);
        } catch (RestClientException e) {
            log.error("[Design Agent] {} 호출 실패: {}", action, e.getMessage(), e);
            throw ApiErrorException.of(ResultCode.Error.API_CALL_FAILED, e);
        }
    }

    /**
     * [전체 초기화] 사용자 편집/검토 결과(Redis 검토/실패 캐시)를 버린다.
     * Agent 호출 없이 캐시만 삭제한다. 이후 조회는 검토 결과가 없는 마지막 영속 상태로 돌아간다.
     * FE는 초기화 후 설계 화면 데이터 조회 API를 재조회한다 (행위 -> 재조회 패턴으로 통일).
     */
    @Override
    public void reset(Long scenarioGroupId) {
        requireEditableByCurrentUser(scenarioGroupId);
        validationStore.delete(scenarioGroupId);
        failureStore.delete(scenarioGroupId);
    }

    /**
     * 편집 소유권 검증 — 설계 세션의 진행자(그룹 request_usr_id)만 편집 API(검토/Signpost/확정/초기화)를 호출할 수 있다.
     * 잠금은 FE 표시만으로는 우회 가능하므로 서버에서 강제한다.
     * 진행자 미상(request_usr_id null)은 Agent 적재 결함이라 편집을 이어가면 오염이 커지므로 fail-closed로 전원 차단한다
     * (화면의 mine 판정과 동일 기준). 이 경우 그룹은 편집 불가가 되며 그룹 삭제 후 재설계로 회복한다.
     */
    private void requireEditableByCurrentUser(Long scenarioGroupId) {
        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %s".formatted(scenarioGroupId)));

        Long userId = authorizationService.getUserNo().longValue();
        if (!userId.equals(group.getRequestUserId())) {
            throw AppHttpStatusException.of(HttpStatus.FORBIDDEN, ResultCode.Error.BIZ_RULE_VIOLATION, "다른 사용자가 진행 중인 설계 세션입니다: %d".formatted(scenarioGroupId));
        }
    }

}
