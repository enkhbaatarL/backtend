package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tmd.custom.CustomUserRepository;

public interface UserRepository extends JpaRepository<User, Integer>, CustomUserRepository {

    @EntityGraph(attributePaths = { "customerCompany" })
    Optional<User> findByUserNoAndIsDeletedFalse(Integer userNo);

    List<User> findByCustomerCompanyAndIsDeletedFalse(CustomerCompany customerCompany);

    Optional<User> findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(String userLoginId, UserRoleCd userRoleCd);

}
