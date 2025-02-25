package com.sk.mi.config;

import com.sk.mi.entity.master.Company;
import com.sk.mi.repository.master.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    
    @Bean
    public CommandLineRunner initData(CompanyRepository companyRepository) {
        return args -> {
            // 기본 회사가 없을 경우에만 생성
            if (companyRepository.count() == 0) {
                Company defaultCompany = new Company();
                defaultCompany.setName("skinnovation");
                companyRepository.save(defaultCompany);
            }
        };
    }
} 