package com.sk.domain.shared.repository.master.tif;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tif.UserInfo;
import com.sk.domain.shared.repository.master.tif.custom.CustomUserInfoRepository;

public interface UserInfoRepository extends JpaRepository<UserInfo, String>, CustomUserInfoRepository {
    List<UserInfo> findByIsDeletedTrue();
}
