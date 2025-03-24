package com.sk.common.config.database.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Master 데이터소스 JPA 설정
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.sk.domain.shared.repository.master", entityManagerFactoryRef = "masterEntityManagerFactory", transactionManagerRef = "masterTransactionManager")
public class MasterJpaConfig {
    @Primary
    @Bean(name = "masterEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean masterEntityManagerFactory(
            @Qualifier("masterDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages("com.sk.domain.shared.entity.master")
                .persistenceUnit("master")
                .build();
    }

    @Primary
    @Bean(name = "masterTransactionManager")
    public PlatformTransactionManager masterTransactionManager(
            @Qualifier("masterEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public JPAQueryFactory jpaQueryFactory(@Qualifier("masterEntityManagerFactory") EntityManagerFactory emf) {
        return new JPAQueryFactory(emf.createEntityManager());
    }
}
