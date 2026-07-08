package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 병합 컴포넌트의 입력
 * 확정 payload(confirmation_parameters)의 항목 중 병합에 필요한 최소만 받는다.
 * 파일/시트/셀 좌표는 payload의 modelTarget이 아니라 마스터(tmm_mi_model_param_m)를 parameterName으로 조회해 해석하므로 여기서는 받지 않는다 — 호출 측 DTO 변경에 결합되지 않는다.
 * values의 각 row는 slot 축 키(마스터 column roles의 라벨과 일치: Year, Month, Country 등)와 메타 필드(basecaseValue/proposedValue/rationale)로 구성된다.
 * 병합은 proposedValue(최종 입력값, 절대값)를 셀에 그대로 기입하며 메타 중 나머지는 무시한다.
 */
public record ModelParamAdjustment(

    String parameterName,

    List<Map<String, Object>> values

) {

    /** slot 축이 아닌 메타 필드 키 — slot 축 추출 시 제외 대상. */
    public static final Set<String> META_FIELDS = Set.of("basecaseValue", "proposedValue", "rationale");

    public static ModelParamAdjustment of(String parameterName, List<Map<String, Object>> values) {
        return new ModelParamAdjustment(parameterName, values);
    }

    /**
     * tmm_mi_model_param_proposal_m.confirmation_parameters JSON array를 병합 입력 목록으로 변환한다.
     * 호출 측은 조회한 컬럼 값을 그대로 넘기면 되고, 병합에 안 쓰는 필드(modelTarget, 파라미터 단위 rationale)는 여기서 버린다.
     * 항목 문제는 "N번째 항목"으로 위치를 특정해 에러로 알린다.
     */
    @SuppressWarnings("unchecked")
    public static List<ModelParamAdjustment> listFromJson(String confirmationParametersJson) {
        if (confirmationParametersJson == null || confirmationParametersJson.isBlank()) {
            throw parseError("확정 파라미터(confirmation_parameters)가 비어 있습니다");
        }

        List<Map<String, Object>> items;
        try {
            items = JsonUtil.fromJson(confirmationParametersJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw parseError("확정 파라미터(confirmation_parameters) JSON 파싱 실패: %s".formatted(e.getMessage()));
        }
        if (items == null || items.isEmpty()) {
            throw parseError("확정 파라미터(confirmation_parameters)가 비어 있습니다");
        }

        List<ModelParamAdjustment> adjustments = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            Map<String, Object> item = items.get(index);
            int position = index + 1;
            if (!(item.get("parameterName") instanceof String parameterName) || parameterName.isBlank()) {
                throw parseError("확정 파라미터 %d번째 항목에 parameterName이 없습니다".formatted(position));
            }
            if (!(item.get("values") instanceof List<?> values) || values.isEmpty()) {
                throw parseError("확정 파라미터 %d번째 항목(%s)에 values가 없습니다".formatted(position, parameterName));
            }
            adjustments.add(of(parameterName, (List<Map<String, Object>>) values));
        }
        return adjustments;
    }

    private static AppErrorException parseError(String detail) {
        return AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "수급모델 입력 병합 실패 — %s".formatted(detail));
    }

}
