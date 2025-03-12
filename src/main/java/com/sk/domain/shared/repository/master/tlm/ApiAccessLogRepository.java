package com.sk.domain.shared.repository.master.tlm;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tlm.ApiAccessLog;

public interface ApiAccessLogRepository extends JpaRepository<ApiAccessLog, Integer> {
}
