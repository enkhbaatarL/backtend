package com.sk.mi.repository.master;

import com.sk.mi.entity.master.OAuth2Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuth2ClientRepository extends JpaRepository<OAuth2Client, String> {
    Optional<OAuth2Client> findByClientId(String clientId);
} 