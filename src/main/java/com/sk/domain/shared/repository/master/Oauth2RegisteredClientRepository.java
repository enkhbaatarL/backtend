package com.sk.domain.shared.repository.master;

import com.sk.domain.shared.entity.master.Oauth2RegisteredClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Oauth2RegisteredClientRepository extends JpaRepository<Oauth2RegisteredClient, Long> {
    Optional<Oauth2RegisteredClient> findByClientId(String clientId);
}
