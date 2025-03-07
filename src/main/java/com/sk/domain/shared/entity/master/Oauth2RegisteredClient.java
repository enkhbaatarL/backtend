package com.sk.domain.shared.entity.master;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "oauth2_registered_client")
public class Oauth2RegisteredClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @Column(name = "tenant", nullable = false)
    private String tenant;

    @Column(name = "authorization_grant_types")
    private String authorizationGrantTypes;

    @Column(name = "client_authentication_method")
    private String clientAuthenticationMethod;

    @Column(name = "scopes")
    private String scopes;

}
