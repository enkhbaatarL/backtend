package com.sk.domain.shared.repository.master;

import com.sk.domain.shared.entity.master.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
} 