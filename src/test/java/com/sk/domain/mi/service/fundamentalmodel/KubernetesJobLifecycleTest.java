package com.sk.domain.mi.service.fundamentalmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.api.model.batch.v1.JobListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.GracePeriodConfigurable;
import io.fabric8.kubernetes.client.dsl.BatchAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.Deletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.fabric8.kubernetes.client.dsl.V1BatchAPIGroupDSL;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KubernetesJobLifecycleTest {

    @Mock private KubernetesClient client;
    @Mock private BatchAPIGroupDSL batch;
    @Mock private V1BatchAPIGroupDSL v1;
    @Mock private MixedOperation<Job, JobList, ScalableResource<Job>> jobs;
    @Mock private NonNamespaceOperation<Job, JobList, ScalableResource<Job>> namespacedJobs;
    @Mock private ScalableResource<Job> resource;
    @Mock private GracePeriodConfigurable<Deletable> foregroundDelete;

    private KubernetesJobServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KubernetesJobServiceImpl(client);
        ReflectionTestUtils.setField(service, "namespace", "mi-model");
        ReflectionTestUtils.setField(service, "modelImage", "model:latest");
        ReflectionTestUtils.setField(service, "ttlSecondsAfterFinished", 3600);
        given(client.batch()).willReturn(batch);
        given(batch.v1()).willReturn(v1);
        given(v1.jobs()).willReturn(jobs);
        given(jobs.inNamespace("mi-model")).willReturn(namespacedJobs);
    }

    @Test
    void createdJobUsesCanonicalNameAndOriginalRunIdMetadata() {
        given(namespacedJobs.resource(any(Job.class))).willReturn(resource);
        String runId = "20260706_120000_A1B2";

        service.createFundamentalModelJob(FundamentalModelJobArgs.builder()
            .caseId(1L).scenarioId(2L).runId(runId).runSeq(3L)
            .demandExcelPath("d").supplyExcelPath("s").tradeExcelPath("t")
            .outputExcelPath("o")
            .callbackUrl("http://callback").build());

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        then(namespacedJobs).should().resource(captor.capture());
        Job job = captor.getValue();
        assertThat(job.getMetadata().getName()).isEqualTo(service.toJobName(runId));
        assertThat(job.getMetadata().getLabels()).containsEntry("mi.sk.com/run-id", runId);
        assertThat(job.getMetadata().getAnnotations()).containsEntry("mi.sk.com/original-run-id", runId);
        assertThat(job.getSpec().getTtlSecondsAfterFinished()).isEqualTo(3600);
        assertThat(job.getSpec().getTemplate().getSpec().getContainers().get(0).getArgs())
            .containsSubsequence(
                "--trade-excel-path", "t",
                "--output-excel-path", "o",
                "--callback-url", "http://callback"
            );
    }

    @Test
    void createdJobOmitsOutputInputArgumentWhenSharedScenarioContractDoesNotProvideIt() {
        given(namespacedJobs.resource(any(Job.class))).willReturn(resource);

        service.createFundamentalModelJob(FundamentalModelJobArgs.builder()
            .scenarioId(2L).runId("20260706_120000_A1B2").runSeq(3L)
            .demandExcelPath("d").supplyExcelPath("s").tradeExcelPath("t")
            .callbackUrl("http://callback").build());

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        then(namespacedJobs).should().resource(captor.capture());
        assertThat(captor.getValue().getSpec().getTemplate().getSpec().getContainers().get(0).getArgs())
            .doesNotContain("--output-excel-path");
    }

    @Test
    void listsOnlyManagedNonTerminalJobsAndSupportsLegacyName() {
        Job active = job("fundamental-model-20260706-120000-a1b2", null);
        Job terminal = job("fundamental-model-20260706-120001-a1b2", "Complete");
        Job unrelated = job("other-job", null);
        given(namespacedJobs.list()).willReturn(new JobListBuilder().withItems(active, terminal, unrelated).build());

        assertThat(service.findManagedActiveJobs())
            .extracting(KubernetesJobService.ManagedActiveJob::runId)
            .containsExactly("20260706_120000_a1b2");
    }

    @Test
    void missingJobDeleteIsIdempotentAndUsesForegroundPropagation() {
        String runId = "20260706_120000_A1B2";
        given(namespacedJobs.withName(service.toJobName(runId))).willReturn(resource);
        org.mockito.Mockito.doReturn(foregroundDelete)
            .when(resource).withPropagationPolicy(DeletionPropagation.FOREGROUND);
        given(foregroundDelete.delete()).willReturn(List.of());

        service.deleteJob(runId);

        then(foregroundDelete).should().delete();
    }

    private Job job(String name, String terminalConditionType) {
        JobBuilder builder = new JobBuilder().withNewMetadata().withName(name).endMetadata();
        if (terminalConditionType == null) {
            return builder.build();
        }
        return builder.withNewStatus()
            .addNewCondition().withType(terminalConditionType).withStatus("True").endCondition()
            .endStatus()
            .build();
    }
}
