package com.sk.domain.shared.repository.master.tmd;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmd.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
