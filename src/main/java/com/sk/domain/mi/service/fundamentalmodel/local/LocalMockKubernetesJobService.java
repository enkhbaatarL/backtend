package com.sk.domain.mi.service.fundamentalmodel.local;

import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import com.sk.domain.mi.service.fundamentalmodel.KubernetesJobService;
import com.sk.domain.shared.entity.master.tmd.ScenarioCasePrice;
import com.sk.domain.shared.repository.master.tmd.ScenarioCasePriceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * [로컬 전용] 수급모델 실행 Job을 Kubernetes 없이 시뮬레이션한다.
 *
 * <p>실제 submit 경로({@code ScenarioModelExecutionServiceImpl#submitModelExecution})가 만든
 * ScenarioCase·run history·업로드 파일은 그대로 두고, K8s Job이 하는 두 가지 —
 * (1) 결과 가격을 {@code tmd_mdl_scnrcaseprcl}에 적재, (2) 완료 콜백 POST — 만 대신 수행한다.
 *
 * <p>가격은 {@code mock/scenarios/scenario-case-price-seed.csv} 를 이번 실행의 run_seq로 치환해 INSERT하며,
 * 콜백은 운영과 동일하게 {@code POST {callbackUrl}} 로 self-call 하여 run/case 상태를 COMPLETED로 전이시킨다.
 * 그 결과 그래프 조회(latest COMPLETED run_seq join)가 seed된 가격을 그대로 반환한다.
 */
@Slf4j
@Service
@Profile("testdb")
@RequiredArgsConstructor
public class LocalMockKubernetesJobService implements KubernetesJobService {

    private static final String SEED_RESOURCE_PATH = "mock/scenarios/scenario-case-price-seed.csv";
    private static final String ACTOR = "SYSTEM";
    private static final DateTimeFormatter CALLBACK_FINISHED_AT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    // CSV 열: prc_seq, run_seq, crt_usr, crt_dt, mod_usr, mod_dt, del_yn, perd_unit, perd_key, prc_rgn_nm, base_prc, user_adj_prc
    private static final int COL_PERIOD_UNIT = 7;
    private static final int COL_PERIOD_KEY = 8;
    private static final int COL_REGION_NAME = 9;
    private static final int COL_BASE_PRICE = 10;
    private static final int COL_USER_ADJ_PRICE = 11;
    private static final int COL_COUNT = 12;

    private final ScenarioCasePriceRepository scenarioCasePriceRepository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final RestClient restClient = RestClient.create();
    /** submit 후 콜백(완료)까지의 시뮬레이션 지연(초). FE 로딩/폴링 확인용. */
    @Value("${mi.next-gen.local-mock.model-callback-delay-seconds:15}")
    private long callbackDelaySeconds;
    private List<SeedRow> seedRows = List.of();

    @PostConstruct
    void init() {
        this.seedRows = loadSeedRows();
        log.info("[LocalMock] Fundamental model K8s job simulation active. seedPriceRows={}, callbackDelaySeconds={}",
            seedRows.size(), callbackDelaySeconds);
    }

    @PreDestroy
    void destroy() {
        scheduler.shutdownNow();
    }

    @Override
    public CompletableFuture<Void> createFundamentalModelJob(FundamentalModelJobArgs args) {
        log.info("[LocalMock] Simulating fundamental model job. runId={}, runSeq={}, callbackUrl={}",
            args.getRunId(), args.getRunSeq(), args.getCallbackUrl());
        scheduler.schedule(() -> simulateCompletion(args), callbackDelaySeconds, TimeUnit.SECONDS);
        return CompletableFuture.completedFuture(null);
    }

    private void simulateCompletion(FundamentalModelJobArgs args) {
        try {
            seedPrices(args.getRunSeq());
            postCompletedCallback(args.getCallbackUrl());
            log.info("[LocalMock] Completed fundamental model job simulation. runId={}, runSeq={}",
                args.getRunId(), args.getRunSeq());
        } catch (RuntimeException e) {
            log.warn("[LocalMock] Fundamental model job simulation failed. runId={}, runSeq={}",
                args.getRunId(), args.getRunSeq(), e);
        }
    }

    private void seedPrices(Long runSeq) {
        if (runSeq == null) {
            log.warn("[LocalMock] runSeq is null. Skip price seeding.");
            return;
        }
        if (seedRows.isEmpty()) {
            log.warn("[LocalMock] No seed price rows loaded. Skip price seeding. runSeq={}", runSeq);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<ScenarioCasePrice> prices = new ArrayList<>(seedRows.size());
        for (SeedRow row : seedRows) {
            prices.add(new ScenarioCasePrice(
                now, now, ACTOR, ACTOR, Boolean.FALSE,
                null, runSeq,
                row.periodUnit(), row.periodKey(), row.regionName(),
                row.baseCasePrice(), row.userAdjustedPrice()
            ));
        }
        scenarioCasePriceRepository.saveAll(prices);
        log.info("[LocalMock] Seeded {} price rows for runSeq={}", prices.size(), runSeq);
    }

    private void postCompletedCallback(String callbackUrl) {
        Map<String, Object> payload = Map.of(
            "status", "COMPLETED",
            "finishedAt", LocalDateTime.now(ZoneOffset.UTC).format(CALLBACK_FINISHED_AT)
        );
        restClient.post()
            .uri(callbackUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toBodilessEntity();
        log.info("[LocalMock] Posted COMPLETED callback. url={}", callbackUrl);
    }

    private List<SeedRow> loadSeedRows() {
        List<SeedRow> rows = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(SEED_RESOURCE_PATH);
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < COL_COUNT) {
                    log.warn("[LocalMock] Skipping malformed seed row: {}", line);
                    continue;
                }
                rows.add(new SeedRow(
                    cols[COL_PERIOD_UNIT].trim(),
                    cols[COL_PERIOD_KEY].trim(),
                    cols[COL_REGION_NAME].trim(),
                    Double.parseDouble(cols[COL_BASE_PRICE].trim()),
                    Double.parseDouble(cols[COL_USER_ADJ_PRICE].trim())
                ));
            }
        } catch (IOException | NumberFormatException e) {
            log.error("[LocalMock] Failed to load price seed resource: {}", SEED_RESOURCE_PATH, e);
            return List.of();
        }
        return List.copyOf(rows);
    }

    @Override
    public String createPermissionTestJob() {
        log.info("[LocalMock] createPermissionTestJob no-op.");
        return "local-mock-permission-test";
    }

    @Override
    public String toJobName(String runId) {
        return "fundamental-model-" + runId.replace('_', '-').toLowerCase();
    }

    @Override
    public List<ManagedActiveJob> findManagedActiveJobs() {
        return List.of();
    }

    @Override
    public void deleteJob(String runId) {
        log.info("[LocalMock] deleteJob no-op. runId={}", runId);
    }

    private record SeedRow(
        String periodUnit,
        String periodKey,
        String regionName,
        Double baseCasePrice,
        Double userAdjustedPrice
    ) {
    }
}
