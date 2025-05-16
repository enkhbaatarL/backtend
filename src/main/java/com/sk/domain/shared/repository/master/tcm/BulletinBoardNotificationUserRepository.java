package com.sk.domain.shared.repository.master.tcm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardUserMappingId;
import com.sk.domain.shared.entity.master.tcm.BulletinBoardNotificationUser;

public interface BulletinBoardNotificationUserRepository extends JpaRepository<BulletinBoardNotificationUser, BulletinBoardUserMappingId> {

    Optional<BulletinBoardNotificationUser> findByIdAndIsDeletedFalse(BulletinBoardUserMappingId id);
}
