package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 수급모델(Fundamental Model) 실행 Job 처리 추상화.
 *
 * <p>운영/개발 환경은 실제 Kubernetes Job을 생성하는 {@link KubernetesJobServiceImpl}(@Profile("!local"))이,
 * 로컬 환경은 K8s 없이 콜백·가격 결과를 시뮬레이션하는
 * {@link com.sk.domain.mi.service.fundamentalmodel.local.LocalMockKubernetesJobService}(@Profile("local"))가 주입된다.
 */
public interface KubernetesJobService {

    /**
     * k8s Job 생성 권한(RBAC) 검증 전용. 최소 Job을 동기 생성해 생성된 Job 이름을 반환한다.
     */
    String createPermissionTestJob();

    /**
     * 수급모델 실행 Job을 생성(비동기 dispatch)한다.
     */
    CompletableFuture<Void> createFundamentalModelJob(FundamentalModelJobArgs args);

    /**
     * runId를 k8s Job 이름 규칙으로 변환한다.
     */
    String toJobName(String runId);

    /**
     * 백엔드가 관리하는(라벨/이름 규칙) 활성 Job 목록을 조회한다.
     */
    List<ManagedActiveJob> findManagedActiveJobs();

    /**
     * runId에 해당하는 Job을 삭제한다. 없으면(404) 무시한다.
     */
    void deleteJob(String runId);

    record ManagedActiveJob(String jobName, String runId) {
    }
}
