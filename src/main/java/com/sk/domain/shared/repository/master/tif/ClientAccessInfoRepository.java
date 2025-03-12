package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.master.tif.ClientAccessInfo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientAccessInfoRepository extends JpaRepository<ClientAccessInfo, Long> {

    @EntityGraph(attributePaths = {"customerCompany"})
    List<ClientAccessInfo> findByIsDeletedFalse();

    Optional<ClientAccessInfo> findByClientAccessRegNoAndIsDeletedFalse(Integer integer);

}
