package com.sk.domain.shared.repository.master.tlm;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.repository.master.tlm.custom.CustomEmailAccessLogRepository;

public interface EmailAccessLogRepository extends JpaRepository<EmailAccessLog, Integer>, CustomEmailAccessLogRepository {
    @EntityGraph(attributePaths = {
            "goodsCriticalValueSetting",
            "goodsCriticalValueSetting.goodsDataSourceDetail"
    })
    Optional<EmailAccessLog> findByEmailSendSnAndEmailReceiveUserLoginIdAndIsDeletedFalse(Integer emailSendSn, String emailReceiveUserLoginId);
}
