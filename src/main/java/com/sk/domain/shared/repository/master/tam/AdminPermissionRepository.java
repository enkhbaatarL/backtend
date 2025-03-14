package com.sk.domain.shared.repository.master.tam;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.compositeKey.tam.AdminPermissionId;
import com.sk.domain.shared.entity.master.tam.AdminPermission;

public interface AdminPermissionRepository extends JpaRepository<AdminPermission, AdminPermissionId> {

    List<AdminPermission> findByIdUserNoAndIsDeletedFalse(Integer userNo);
}
