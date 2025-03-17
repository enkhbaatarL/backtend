package com.sk.domain.shared.repository.master.tlm;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.repository.master.tlm.custom.CustomEmailAccessLogRepository;

public interface EmailAccessLogRepository extends JpaRepository<EmailAccessLog, Integer>, CustomEmailAccessLogRepository {

}
