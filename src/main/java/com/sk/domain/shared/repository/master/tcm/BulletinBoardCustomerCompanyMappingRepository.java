package com.sk.domain.shared.repository.master.tcm;

import java.lang.StackWalker.Option;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.bbs.BulletinBoardCompanyDto;
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
            DELETE FROM BulletinBoardCustomerCompanyMapping b where b.bulletinBoard.bulletinNo = :bulletinNo
                        """)
    void deleteByBulletinNo(@Param("bulletinNo") Integer bulletinNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.bbs.BulletinBoardCompanyDto(
                        c.customerCompanyNo,
                        c.customerCompanyNm
                                    )
                                    FROM BulletinBoardCustomerCompanyMapping m
                                    JOIN m.customerCompany c
                                    WHERE c.isDeleted = false
                                    AND m.bulletinBoard = :bulletinBoard
                                                """)
    List<BulletinBoardCompanyDto> findCustomerCompanyByBulletinBoard(BulletinBoard bulletinBoard);

    Optional<BulletinBoardCustomerCompanyMapping> findById(BulletinBoardCustomerCompanyMappingId id);
}
