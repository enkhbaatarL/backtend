package com.sk.domain.shared.repository.master.tim;

import com.sk.domain.shared.entity.master.tlm.ApiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiAccessLogRepository extends JpaRepository<ApiAccessLog, Long> {
}
