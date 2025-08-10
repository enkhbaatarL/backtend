package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.master.tif.ClientAccessInfo;
import com.sk.domain.shared.repository.master.tif.custom.CustomClientAccessInfoRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientAccessInfoRepository extends JpaRepository<ClientAccessInfo, Long>, CustomClientAccessInfoRepository {

    @EntityGraph(attributePaths = {"customerCompany"})
    Optional<ClientAccessInfo> findByClientAccessRegNoAndIsDeletedFalse(Integer integer);

    @EntityGraph(attributePaths = {"customerCompany"})
    Optional<ClientAccessInfo> findByClientCertIdAndIsDeletedFalse(String clientId);

}
