package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardCustomerCompanyMappingId;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tcm.BulletinBoardCustomerCompanyMapping;

import jakarta.transaction.Transactional;

public interface BulletinBoardCustomerCompanyMappingRepository
        extends JpaRepository<BulletinBoardCustomerCompanyMapping, BulletinBoardCustomerCompanyMappingId> {

    List<BulletinBoardCustomerCompanyMapping> findByBulletinBoard(BulletinBoard bulletinBoard);

    @Transactional
    @Modifying
    @Query(value = """
            delete from BulletinBoardCustomerCompanyMapping b where b.bulletinBoard.bulletinNo = :bulletinNo
                        """)
    void deleteByBulletinNo(@Param("bulletinNo") Integer bulletinNo);

}
