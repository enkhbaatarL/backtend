package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.client.FileCryptoApiClient;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import com.sk.domain.shared.service.FileEncryptionPolicy;
import com.sk.domain.shared.service.ObjectStorageService;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/** 웹 접수와 전용 worker가 같은 master 저장/파일 경계를 명시적으로 조립한다. 생성 시 IO는 없다. */
@Configuration(proxyBeanMethods=false)
class InputDraftFinalizationConfiguration {
    @Bean InputDraftFinalizationService inputDraftFinalizations(@Qualifier("masterDataSource") DataSource master,
            ModelInputDraftRepository common,ModelInputDraftService drafts,InputDraftJson json,
            InputWorkbookReader reader,InputTempFiles scratch,ObjectStorageService storage,
            FileEncryptionPolicy protection,FileCryptoApiClient crypto,Environment env,
            PlatformTransactionManager manager) {
        var repository=new InputDraftFinalizationRepository(master,common,json,drafts::verifyFinalizationBasis);
        var files=new InputDraftFinalizationFiles(reader,scratch,
            (job,role,original,target)->{
                if(job.basis()==null)InputDraftPreserveChange.replay(job,role,original,target,reader,json);
                else new InputWorkbookEditor(reader,scratch,json,()->{if(Thread.currentThread().isInterrupted())throw new InputDraftPreserveChange.Failure("TIMEOUT");})
                    .finalFile(job.basis(),role,original,target);
            });
        var objects=new InputDraftFinalizationStorage(storage,protection,crypto,
            env.getRequiredProperty("ncp.storage.end-point"),env.getRequiredProperty("ncp.storage.bucket-name"),env.getRequiredProperty("ncp.storage.prefix"));
        return new InputDraftFinalizationService(repository,files,objects,manager);
    }
}
