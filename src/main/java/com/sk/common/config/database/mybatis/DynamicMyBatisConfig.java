package com.sk.common.config.database.mybatis;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * Master를 제외한 다른 테넌트 사용하는 MyBatis 설정
 */
@Configuration
@MapperScan(basePackages = "com.sk.domain.shared.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class DynamicMyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(
            @Qualifier("dynamicDataSource") DataSource dynamicDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dynamicDataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mybatis/mapper/**/*.xml"));
        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(
            SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "myBatisTransactionManager")
    public DataSourceTransactionManager myBatisTransactionManager(
            @Qualifier("dynamicDataSource") DataSource dynamicDataSource) {
        return new DataSourceTransactionManager(dynamicDataSource);
    }
}
