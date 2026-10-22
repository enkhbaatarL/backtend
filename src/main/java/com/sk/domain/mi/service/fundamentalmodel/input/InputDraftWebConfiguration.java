package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/** 활성화 속성 하나, 기본 꺼짐. 기존 기능 속성은 실제 작업 선택에 적용한다. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "mi.model-input.web-runner.enabled", havingValue = "true")
class InputDraftWebConfiguration {
    @Bean InputDraftWebRunner inputDraftWebRunner(@Qualifier("masterDataSource") DataSource master,
            PlatformTransactionManager manager, ModelInputDraftRepository common, ModelInputDraftService drafts,
            InputUploadService uploads, InputDraftFinalizationService finalizations, InputWorkbookReader reader,
            InputTempFiles scratch, InputDraftJson json, Environment env) {
        return create(master, manager, common, drafts, uploads, finalizations, reader, scratch, json, env);
    }

    static InputDraftWebRunner create(DataSource master, PlatformTransactionManager manager,
            ModelInputDraftRepository common, ModelInputDraftService drafts, InputUploadService uploads,
            InputDraftFinalizationService finalizations, InputWorkbookReader reader, InputTempFiles scratch,
            InputDraftJson json, Environment env) {
        var tasks = new InputDraftWebTasks(common, drafts, uploads, finalizations, reader, scratch, json, manager,
                env.getProperty("mi.model-input.change-worker.enabled", Boolean.class, false),
                env.getProperty("mi.model-input.finalization.enabled", Boolean.class, false));
        return new InputDraftWebRunner(InputDraftRunnerLease.mysql(master), new InputDraftRunnerRepository(master, manager), tasks, scratch);
    }
}
