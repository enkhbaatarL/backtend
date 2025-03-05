package com.sk.domain.shared.repository.master;

import com.sk.domain.shared.entity.master.User;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByUsername(String username);

  Optional<User> findByUsername(String username);
}
