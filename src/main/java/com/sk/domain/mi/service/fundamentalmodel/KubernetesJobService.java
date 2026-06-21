package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KubernetesJobService {

    private static final int JOB_BACKOFF_LIMIT = 0;

    // python3 -c 방식으로 실행할 smoke 스크립트
    // --run-id, --callback-url arg를 파싱해 callback POST 후 종료
    private static final String SMOKE_SCRIPT = """
            import sys, time, urllib.request, json, datetime
            def get_arg(a, k):
                try: return a[a.index(k) + 1]
                except: return None
            a = sys.argv[1:]
            run_id = get_arg(a, '--run-id')
            callback_url = get_arg(a, '--callback-url')
            print(f'[smoke] run_id={run_id}')
            print(f'[smoke] callback_url={callback_url}')
            time.sleep(2)
            if callback_url:
                now = datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%S')
                p = json.dumps({'status': 'COMPLETED', 'finishedAt': now}).encode()
                req = urllib.request.Request(callback_url, data=p, headers={'Content-Type': 'application/json'}, method='POST')
                try:
                    with urllib.request.urlopen(req, timeout=10) as r:
                        print(f'[smoke] callback status={r.status}')
                except Exception as e:
                    print(f'[smoke] callback error={e}')
            print('[smoke] done')
            """;
    private final KubernetesClient kubernetesClient;
    @Value("${kubernetes.namespace}")
    private String namespace;
    @Value("${kubernetes.image}")
    private String modelImage;
    // smoke용: "python3,-c" / 실제 모델 이미지 사용 시 비워둠
    @Value("${kubernetes.command:}")
    private String commandConfig;
    @Value("${kubernetes.env-config-map-name:}")
    private String envConfigMapName;
    @Value("${kubernetes.env-secret-name:}")
    private String envSecretName;
    @Value("${kubernetes.image-pull-secret-name:}")
    private String imagePullSecretName;
    @Value("${kubernetes.ttl-seconds-after-finished:3600}")
    private Integer ttlSecondsAfterFinished;

    // k8s Job 생성 권한(RBAC) 검증 전용. 최소 Job을 동기 생성해 생성된 Job 이름을 반환한다.
    // 동기 응답은 create API 권한만 검증하며, 이미지 pull 결과는 생성된 pod 이벤트에서 확인한다.
    public String createPermissionTestJob() {
        String jobName = "k8s-permission-test-" + System.currentTimeMillis();
        Job job = new JobBuilder()
            .withNewMetadata()
                .withName(jobName)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withBackoffLimit(JOB_BACKOFF_LIMIT)
                .withTtlSecondsAfterFinished(ttlSecondsAfterFinished)
                .withNewTemplate()
                    .withNewSpec()
                        .withImagePullSecrets(buildImagePullSecrets())
                        .addNewContainer()
                            .withName("permission-test")
                            .withImage(modelImage)
                            .withCommand("sh", "-c", "echo k8s-permission-test-ok")
                        .endContainer()
                        .withRestartPolicy("Never")
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();

        kubernetesClient.batch().v1().jobs()
            .inNamespace(namespace)
            .resource(job)
            .create();

        return jobName;
    }

    @Async
    public CompletableFuture<Void> createFundamentalModelJob(FundamentalModelJobArgs args) {
        Job job = new JobBuilder()
            .withNewMetadata()
                .withName("fundamental-model-" + args.getRunId().replace('_', '-').toLowerCase())
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withBackoffLimit(JOB_BACKOFF_LIMIT)
                .withTtlSecondsAfterFinished(ttlSecondsAfterFinished)
                .withNewTemplate()
                    .withNewSpec()
                        .withImagePullSecrets(buildImagePullSecrets())
                        .withContainers(buildContainer(args))
                        .withRestartPolicy("Never")
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();

        kubernetesClient.batch().v1().jobs()
            .inNamespace(namespace)
            .resource(job)
            .create();

        return CompletableFuture.completedFuture(null);
    }

    private Container buildContainer(FundamentalModelJobArgs args) {
        List<String> namedArgs = new ArrayList<>(List.of(
            "--run-id",            args.getRunId(),
            "--run-seq",           String.valueOf(args.getRunSeq()),
            "--demand-excel-path", args.getDemandExcelPath(),
            "--supply-excel-path", args.getSupplyExcelPath(),
            "--trade-excel-path",  args.getTradeExcelPath(),
            "--callback-url",      args.getCallbackUrl()
        ));
        if (args.getCaseId() != null) {
            namedArgs.addAll(List.of("--case-id", String.valueOf(args.getCaseId())));
        }
        if (args.getScenarioId() != null) {
            namedArgs.addAll(List.of("--scenario-id", String.valueOf(args.getScenarioId())));
        }

        ContainerBuilder builder = new ContainerBuilder()
            .withName("fundamental-model")
            .withImage(modelImage);

        applyEnvFrom(builder);

        if (StringUtils.hasText(commandConfig)) {
            List<String> argsWithScript = new ArrayList<>();
            argsWithScript.add(SMOKE_SCRIPT);
            argsWithScript.addAll(namedArgs);
            builder.withCommand(Arrays.asList(commandConfig.split(",")))
                   .withArgs(argsWithScript);
        } else {
            builder.withArgs(namedArgs);
        }

        return builder.build();
    }

    private List<LocalObjectReference> buildImagePullSecrets() {
        if (!StringUtils.hasText(imagePullSecretName)) {
            return null;
        }

        return List.of(new LocalObjectReferenceBuilder()
            .withName(imagePullSecretName)
            .build());
    }

    private void applyEnvFrom(ContainerBuilder builder) {
        List<EnvFromSource> envFromSources = new ArrayList<>();

        if (StringUtils.hasText(envConfigMapName)) {
            envFromSources.add(new EnvFromSourceBuilder()
                .withNewConfigMapRef()
                    .withName(envConfigMapName)
                .endConfigMapRef()
                .build());
        }

        if (StringUtils.hasText(envSecretName)) {
            envFromSources.add(new EnvFromSourceBuilder()
                .withNewSecretRef()
                    .withName(envSecretName)
                .endSecretRef()
                .build());
        }

        if (!envFromSources.isEmpty()) {
            builder.withEnvFrom(envFromSources);
        }
    }
}
