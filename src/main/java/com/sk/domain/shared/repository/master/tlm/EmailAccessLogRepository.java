package com.sk.domain.shared.repository.master.tlm;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.repository.master.tlm.custom.CustomEmailAccessLogRepository;

public interface EmailAccessLogRepository extends JpaRepository<EmailAccessLog, Integer>, CustomEmailAccessLogRepository {
    Page<EmailAccessLog> findAllByEmailReceiveUserLoginIdAndIsDeletedFalse(Pageable pageable, String emailReceiveUserLoginId);

    Optional<EmailAccessLog> findByEmailSendSnAndEmailReceiveUserLoginIdAndIsDeletedFalse(Integer emailSendSn, String emailReceiveUserLoginId);
}
