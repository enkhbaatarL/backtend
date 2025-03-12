package com.sk.domain.shared.entity.master;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
//@Entity
@Table(name = "oauth2_client_permissions")
public class Oauth2ClientPermission {

    @Id
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Oauth2RegisteredClient registeredClient;

    @Column(name = "permission", nullable = false)
    private String permission;

}
