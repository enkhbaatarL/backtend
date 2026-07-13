package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiModelParam;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 수급모델 파라미터 마스터 조회 (Agent 소유 테이블 read-only).
 *
 * 확정값 Excel 병합 시 payload의 parameterName으로 셀 좌표 해석 규칙을 찾는 용도다.
 * seed 적재/변경 주체는 Agent이며 Backend는 write하지 않는다 (엔티티 Immutable).
 */
public interface MiModelParamRepository extends JpaRepository<MiModelParam, Long> {

    /**
     * 확정 payload의 parameterName들로 병합 좌표 규칙 일괄 조회 (parameter_name은 UNIQUE 자연키)
     */
    List<MiModelParam> findByParameterNameInAndIsDeletedFalse(Collection<String> parameterNames);

}
