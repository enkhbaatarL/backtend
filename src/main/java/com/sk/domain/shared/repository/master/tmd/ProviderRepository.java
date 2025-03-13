package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {

    Optional<Provider> findByProviderNoAndIsDeletedFalse(Integer providerNo);

}

