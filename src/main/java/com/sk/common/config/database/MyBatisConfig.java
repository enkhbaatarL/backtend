package com.sk.common.config.database;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * Master를 제외한 다른 테넌트 사용하는 MyBatis 설정
 */
@Configuration
@MapperScan(
    basePackages = "com.sk.domain.shared.mapper",
    sqlSessionFactoryRef = "sqlSessionFactory"
)
public class MyBatisConfig {

    private final static String MAPPER_LOCATIONS = "classpath:mybatis/mapper/**/*.xml";

    @Bean
    public SqlSessionFactory sqlSessionFactory(@Qualifier("tenantDataSource") DataSource tenantDatasource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(tenantDatasource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATIONS));
        return factoryBean.getObject();
    }

    @Bean(name = "myBatisTransactionManager")
    public DataSourceTransactionManager myBatisTransactionManager(@Qualifier("tenantDataSource") DataSource tenantDatasource) {
        return new DataSourceTransactionManager(tenantDatasource);
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}
