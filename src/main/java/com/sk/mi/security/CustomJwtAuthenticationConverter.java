package com.sk.mi.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class CustomJwtAuthenticationConverter extends JwtAuthenticationConverter {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt){
        List<GrantedAuthority> authorities = new ArrayList<>();

        String role = jwt.getClaimAsString("role");
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions != null) {
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }

//    @Override
//    public Collection<GrantedAuthority> convert(Jwt jwt){
//        List<GrantedAuthority> authorities = new ArrayList<>();
//
//        String role = jwt.getClaimAsString("role");
//        if (role != null) {
//            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
//        }
//
//        List<String> permissions = jwt.getClaimAsStringList("permissions");
//        if (permissions != null) {
//            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
//        }
//
//        return authorities;
//    }
}
