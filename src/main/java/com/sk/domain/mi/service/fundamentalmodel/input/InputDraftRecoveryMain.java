package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/** 운영자 전용 1회 명령. 웹/API/기존 supervisor를 시작하거나 종료시키지 않는다. */
public final class InputDraftRecoveryMain {
    private InputDraftRecoveryMain() {}

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(InputDraftWorkerMain.WorkerConfiguration.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setDefaultProperties(Map.of("mi.model-input.worker-process", "true", "mi.model-input.web-runner.enabled", "false",
                "spring.main.banner-mode", "off", "logging.level.root", "WARN"));
        try (var context = app.run(args)) {
            DataSource master = context.getBean("masterDataSource", DataSource.class);
            var manager = context.getBean(PlatformTransactionManager.class);
            var state = new InputDraftRunnerRepository(master, manager);
            Environment env = context.getEnvironment();
            if (env.getRequiredProperty("runner.action").equals("status")) {
                var current = state.read();
                System.out.printf("execution=%s state=%s operation=%s attempt=%s%n", current.execution(), current.status(), current.operation(), current.token());
                return;
            }
            if (!env.getRequiredProperty("runner.action").equals("recover")
                    || !env.getProperty("runner.confirm-stopped", Boolean.class, false)) {
                throw new IllegalArgumentException("recovery requires explicit actual process and transfer termination confirmation");
            }
            boolean idle = env.getProperty("runner.idle", Boolean.class, false);
            String operation = env.getProperty("runner.operation"), token = env.getProperty("runner.token");
            if (idle ? operation != null || token != null : operation == null || token == null) {
                throw new IllegalArgumentException("specify exact operation/token or explicit idle target");
            }
            var expected = new InputDraftRunnerRepository.State(env.getRequiredProperty("runner.execution"), null, operation, token);
            var runner = InputDraftWebConfiguration.create(master, manager, context.getBean(ModelInputDraftRepository.class),
                    context.getBean(ModelInputDraftService.class), context.getBean(InputUploadService.class),
                    context.getBean(InputDraftFinalizationService.class), context.getBean(InputWorkbookReader.class),
                    context.getBean(InputTempFiles.class), context.getBean(InputDraftJson.class), env);
            runner.recover(expected, env.getRequiredProperty("runner.actor"));
        }
    }
}
