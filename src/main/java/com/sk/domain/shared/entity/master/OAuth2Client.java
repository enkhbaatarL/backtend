//package com.sk.domain.shared.entity.master;
//
//import java.util.Set;
//
//import com.sk.common.converter.StringSetConverter;
//import org.springframework.security.oauth2.core.AuthorizationGrantType;
//import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
//
//import jakarta.persistence.CollectionTable;
//import jakarta.persistence.Column;
//import jakarta.persistence.Convert;
//import jakarta.persistence.ElementCollection;
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import lombok.Getter;
//import lombok.Setter;
//
//@Entity
//@Table(name = "oauth2_registered_client")
//@Getter
//@Setter
//public class OAuth2Client {
//    @Id
//    private String id;
//    private String clientId;
//    private String clientSecret;
//
//    // Client Credentials Grant 필수 정보
//    @Column(length = 1000)
//    @Convert(converter = StringSetConverter.class)
//    private Set<String> scopes;
//
//    // 추가 필드
//    @Column(name = "authorization_grant_types")
//    private String authorizationGrantTypes = AuthorizationGrantType.CLIENT_CREDENTIALS.getValue();
//
//    @Column(name = "client_authentication_method")
//    private String clientAuthenticationMethod = ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue();
//
//    // 커스텀 필드 (권한 관리용)
//    @Column(name = "tenant")
//    private String tenant;
//
//    @ElementCollection(fetch = FetchType.EAGER)
//    @CollectionTable(name = "oauth2_client_permissions")
//    @Column(name = "permission")
//    private Set<String> permissions;
//}
