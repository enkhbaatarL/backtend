package com.sk.domain.shared.entity.master;

import com.sk.domain.shared.enums.Permission;
import com.sk.domain.shared.enums.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Entity
@Table(name = "users")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String username;
  private String password;

  @Enumerated(EnumType.STRING)
  private Role role;

  // TODO: 인증 테스트 후 permission은 DB 테이블로 변경
  @ElementCollection(targetClass = Permission.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "user_permission", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  private Set<Permission> permissions;

  @ManyToOne
  @JoinColumn(name = "company_id")
  private Company company;

  public Set<GrantedAuthority> getAuthorities() {
    return permissions.stream()
        .map(permission -> new SimpleGrantedAuthority(permission.name()))
        .collect(Collectors.toSet());
  }
}
