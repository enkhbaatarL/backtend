package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.custom.CustomUserRepository;

public interface UserRepository extends JpaRepository<User, Integer>, CustomUserRepository {

    @EntityGraph(attributePaths = { "customerCompany" })
    Optional<User> findByUserNoAndIsDeletedFalse(Integer userNo);

    List<User> findByCustomerCompanyAndIsDeletedFalse(CustomerCompany customerCompany);

    Page<User> findByCustomerCompanyAndIsDeletedFalse(CustomerCompany customerCompany, Pageable pageable);

    List<User> findByUserNoInAndIsDeletedFalse(List<Integer> ids);

}
