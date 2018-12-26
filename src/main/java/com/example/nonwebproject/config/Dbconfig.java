package com.example.nonwebproject.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class Dbconfig {
    @Value("${spring.datasource.driver-class-name}")
    private String primaryDriverClassName;

    @Value("${spring.datasource.url}")
    private String primaryUrl;

    @Value("${spring.datasource.username}")
    private String primaryUsername;

    @Value("${spring.datasource.password}")
    private String primaryPassword;

    @Bean(name="primaryDataSource")
    @Primary
    @ConfigurationProperties(prefix="spring.datasource")
    public DataSource primaryDataSource(){
        //return DataSourceBuilder.create().build();

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(primaryDriverClassName);
        dataSource.setUrl(primaryUrl);
        dataSource.setUsername(primaryUsername);
        dataSource.setPassword(primaryPassword);

        return dataSource;
    }

    @Bean(name = "jdbcPrimaryTemplate")
    @Autowired
    public JdbcTemplate jdbcPrimaryTemplate(@Qualifier(value = "primaryDataSource") DataSource primaryDataSource) {
        return new JdbcTemplate(primaryDataSource);
    }
}
