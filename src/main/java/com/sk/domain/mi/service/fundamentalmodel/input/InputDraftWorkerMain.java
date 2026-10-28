package com.sk.domain.mi.service.fundamentalmodel.input;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.properties.DataSourceProperties;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import com.sk.domain.shared.service.ObjectStorageService;
import com.sk.domain.shared.service.ObjectStorageUploadDiagnostics;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/** supervisor의 유일한 자식. 웹 애플리케이션/공용 scheduler는 시작하지 않는다. */
public final class InputDraftWorkerMain {
    public static void main(String[] args) {
        if(!"true".equals(System.getenv("MI_INPUT_DRAFT_SUPERVISED")))throw new IllegalStateException("supervisor required");
        if(ProcessHandle.current().parent().map(ProcessHandle::pid).orElse(-1L)!=1)throw new IllegalStateException("supervisor PID1 required");
        try {InputDraftWorkerSupervisor.verifyBudget(Path.of("/sys/fs/cgroup"));}
        catch(Exception ex){throw new IllegalStateException("worker cgroup unavailable");}
        System.setProperty("mi.model-input.worker-process","true");
        SpringApplication app=new SpringApplication(WorkerConfiguration.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setDefaultProperties(Map.of("spring.main.banner-mode","off","logging.level.root","OFF"));
        try(var context=app.run(args)) {
            DataSource dataSource=context.getBean("masterDataSource",DataSource.class);
            try(Connection lease=dataSource.getConnection()) {
                lease.setAutoCommit(true);
                try(var statement=lease.prepareStatement("SELECT GET_LOCK(CONCAT('mi-input-draft:',DATABASE()),0)")) {
                    statement.setQueryTimeout(5);
                    try(var rows=statement.executeQuery()){if(!rows.next()||rows.getInt(1)!=1)throw new IllegalStateException("worker already active");}
                }
                var worker=new InputDraftChangeWorker(context.getBean(ModelInputDraftRepository.class),context.getBean(ModelInputDraftService.class),
                    context.getBean(InputWorkbookReader.class),context.getBean(InputTempFiles.class),context.getBean(InputDraftJson.class),
                    context.getBean(PlatformTransactionManager.class),Path.of(System.getenv("MI_INPUT_DRAFT_WORK_DIR")),()->Runtime.getRuntime().halt(124),
                    context.getBean(InputDraftFinalizationService.class),context.getEnvironment().getProperty("mi.model-input.finalization.enabled",Boolean.class,false));
                worker.uploads(context.getBean(InputUploadService.class));
                try(worker) {
                    worker.recoverStopped();
                    while(!Thread.currentThread().isInterrupted()) {
                        if(!lease.isValid(2))throw new IllegalStateException("worker lease lost");
                        worker.recoverStopped();
                        if(!worker.runOne())Thread.sleep(500);
                    }
                }
            }
        } catch(Exception ex){Runtime.getRuntime().halt(1);}
    }

    @Configuration(proxyBeanMethods=false)
    @ConditionalOnProperty(name="mi.model-input.worker-process",havingValue="true")
    @EnableConfigurationProperties({com.sk.domain.shared.config.FileEncryptionProperties.class,com.sk.domain.shared.config.FileCryptoApiProperties.class})
    static class WorkerCryptoConfiguration {
        @Bean com.sk.domain.shared.service.FileEncryptionPolicy workerFileProtection(com.sk.domain.shared.config.FileEncryptionProperties properties) {
            return new com.sk.domain.shared.service.FileEncryptionPolicy(properties);
        }
        @Bean com.sk.domain.shared.client.FileCryptoApiClient workerFileCrypto(com.sk.domain.shared.config.FileCryptoApiProperties properties) {
            // 기존 공통 암복호화의 한 client만 조립한다. 다른 외부 API bean/웹 설정은 시작하지 않는다.
            return new com.sk.domain.shared.client.FileCryptoApiClient(new com.sk.domain.shared.client.config.RestClientHttpApiConfig().fileCryptoRestClient(properties),properties);
        }
    }

    @Configuration(proxyBeanMethods=false)
    @ConditionalOnProperty(name="mi.model-input.worker-process",havingValue="true")
    @EnableConfigurationProperties(DataSourceProperties.class)
    @Import({ModelInputDraftRepository.class,ModelInputDraftService.class,InputDraftJson.class,InputWorkbookReader.class,
        InputTableSnapshotReader.class,
        InputTempFiles.class,ObjectStorageService.class,InputDraftFinalizationConfiguration.class,WorkerCryptoConfiguration.class,
        InputUploadRepository.class,InputUploadObjects.class,InputUploadValidation.class,InputUploadService.class})
    static class WorkerConfiguration {
        @Bean(name="masterDataSource",destroyMethod="close")
        HikariDataSource masterDataSource(DataSourceProperties properties) {
            var source=properties.getMaster();var pool=new HikariDataSource();
            pool.setJdbcUrl(source.getJdbcUrl());pool.setUsername(source.getUsername());pool.setPassword(source.getPassword());
            pool.setDriverClassName(source.getDriverClassName());pool.setMaximumPoolSize(4);pool.setMinimumIdle(0);
            pool.setConnectionTimeout(5000);pool.setValidationTimeout(2000);pool.setPoolName("input-draft-master");return pool;
        }
        @Bean ObjectMapper objectMapper(){return new ObjectMapper();}
        @Bean LocalContainerEntityManagerFactoryBean workerEntityManagerFactory(@Qualifier("masterDataSource") DataSource source) {
            var factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(source);
            factory.setPackagesToScan("com.sk.inputdraftworker.persistence");factory.setPersistenceUnitName("input-draft-master");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","none","hibernate.show_sql","false"));return factory;
        }
        @Bean PlatformTransactionManager masterTransactionManager(LocalContainerEntityManagerFactoryBean factory) {
            return new JpaTransactionManager(java.util.Objects.requireNonNull(factory.getObject()));
        }
        @Bean(destroyMethod="shutdown") AmazonS3 workerStorage(Environment env) {
            return AmazonS3ClientBuilder.standard().withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                env.getRequiredProperty("ncp.storage.end-point"),env.getRequiredProperty("ncp.storage.region-name")))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(env.getRequiredProperty("ncp.storage.access-key"),env.getRequiredProperty("ncp.storage.secret-key"))))
                .withRequestHandlers(new ObjectStorageUploadDiagnostics())
                .withClientConfiguration(new ClientConfiguration().withConnectionTimeout(5000).withSocketTimeout(10000)
                    .withRequestTimeout(15000).withClientExecutionTimeout(20000).withMaxErrorRetry(0)).build();
        }
    }
}
