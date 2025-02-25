package com.sk.mi.repository.master;

import com.sk.mi.entity.master.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
} 