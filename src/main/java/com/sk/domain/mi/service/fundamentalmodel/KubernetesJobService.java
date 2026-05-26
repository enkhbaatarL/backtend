package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KubernetesJobService {

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

    public void createFundamentalModelJob(FundamentalModelJobArgs args) {
        Job job = new JobBuilder()
            .withNewMetadata()
                .withName("fundamental-model-" + args.getRunId().replace('_', '-').toLowerCase())
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withTtlSecondsAfterFinished(0)
                .withNewTemplate()
                    .withNewSpec()
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
    }

    private Container buildContainer(FundamentalModelJobArgs args) {
        List<String> namedArgs = List.of(
            "--run-id",            args.getRunId(),
            "--run-seq",           String.valueOf(args.getRunSeq()),
            "--demand-excel-path", args.getDemandExcelPath(),
            "--supply-excel-path", args.getSupplyExcelPath(),
            "--trade-excel-path",  args.getTradeExcelPath(),
            "--callback-url",      args.getCallbackUrl()
        );

        ContainerBuilder builder = new ContainerBuilder()
            .withName("fundamental-model")
            .withImage(modelImage);

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
}
