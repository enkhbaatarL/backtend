package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.AxpRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AxpRepositoryRepository extends JpaRepository<AxpRepository, String> {
}
