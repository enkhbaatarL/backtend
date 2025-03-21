package com.sk.domain.shared.repository.master.tcm;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.bbs.NoticeRes;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;

public interface BulletinBoardRepository extends JpaRepository<BulletinBoard, Integer> {

    /*******************************************************************
    * 
    * 공통
    * 
    *******************************************************************/

    Optional<BulletinBoard> findByBulletinNoAndBulletinTypeCodeAndIsDeletedFalse(Integer bulletinNo, String bulletinTypeCode);

    /*******************************************************************
    * 
    * 공지사항
    * 
    *******************************************************************/

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.bbs.NoticeRes(
                b.bulletinNo,
                b.isTopFixed,
                b.isPopupUse,
                CASE
                    WHEN b.bulletinCategorySeparateCode = 'CAT1' THEN '카테고리1'
                    WHEN b.bulletinCategorySeparateCode = 'CAT2' THEN '카테고리2'
                    WHEN b.bulletinCategorySeparateCode = 'CAT3' THEN '카테고리3'
                END,
                b.bulletinTitleName,
                cu.userNm,
                b.createDt,
                CASE
                    WHEN b.bulletinValidBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                         AND b.bulletinValidEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                    THEN '게시중'
                    ELSE '게시안함'
                END,
                COALESCE(CONCAT(FUNCTION('GROUP_CONCAT', c.customerCompanyNm), ''), '전체')
            )
            FROM BulletinBoard b
            LEFT JOIN BulletinBoardCustomerCompanyMapping m ON m.bulletinBoard = b
            LEFT JOIN m.customerCompany c
            LEFT JOIN User cu ON b.createUserId = cu.userLoginId
            WHERE b.isDeleted = false
              AND b.bulletinTypeCode = 'NOTICE'
              AND (:category IS NULL OR b.bulletinCategorySeparateCode = :category)
              AND (:status IS NULL OR (
                  (:status = 'ACTIVE' AND b.bulletinValidBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                   AND b.bulletinValidEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d'))
                  OR (:status = 'INACTIVE' AND (b.bulletinValidBeginDt > FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                   OR b.bulletinValidEndDt < FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')))
              ))
              AND (:target IS NULL OR c.customerCompanyNo = :target)
              AND (:keyword IS NULL OR (
                  LOWER(b.bulletinTitleName) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(b.bulletinContent) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            GROUP BY
                b.bulletinNo,
                b.isTopFixed,
                b.isPopupUse,
                b.bulletinCategorySeparateCode,
                b.bulletinTitleName,
                cu.userNm,
                b.createDt,
                b.bulletinValidBeginDt,
                b.bulletinValidEndDt
            ORDER BY b.createDt DESC
            """, countQuery = """
            SELECT COUNT(DISTINCT b.bulletinNo)
            FROM BulletinBoard b
            LEFT JOIN BulletinBoardCustomerCompanyMapping m ON m.bulletinBoard = b
            LEFT JOIN m.customerCompany c
            LEFT JOIN User cu ON b.createUserId = cu.userLoginId
            WHERE b.isDeleted = false
            AND b.bulletinTypeCode = 'NOTICE'
            AND (:category IS NULL OR b.bulletinCategorySeparateCode = :category)
            AND (:status IS NULL OR (
                (:status = 'ACTIVE' AND b.bulletinValidBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND b.bulletinValidEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d'))
                OR (:status = 'INACTIVE' AND (b.bulletinValidBeginDt > FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                OR b.bulletinValidEndDt < FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')))
            ))
            AND (:target IS NULL OR c.customerCompanyNo = :target)
            AND (:keyword IS NULL OR (
                LOWER(b.bulletinTitleName) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(b.bulletinContent) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            """)
    Page<NoticeRes> searchNotice(
            @Param("category") String category,
            @Param("status") String status,
            @Param("target") Integer target,
            @Param("keyword") String keyword,
            Pageable pageable);

    /*******************************************************************
    * 
    * FAQ
    * 
    *******************************************************************/
    @Query(value = """
            SELECT b
            FROM BulletinBoard b
            LEFT JOIN BulletinBoardCustomerCompanyMapping m ON m.bulletinBoard = b
            LEFT JOIN m.customerCompany c
            WHERE b.isDeleted = false
            AND b.bulletinTypeCode = 'NOTICE'
            AND (:category IS NULL OR (b.bulletinCategorySeparateCode = :category))
            AND (:status IS NULL OR (
            (:status = 'ACTIVE' AND b.bulletinValidBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d') AND b.bulletinValidEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d'))
            OR
            (:status = 'INACTIVE' AND (b.bulletinValidBeginDt > FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d') OR b.bulletinValidEndDt < FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')))
            ))
            AND (:target IS NULL OR (c.customerCompanyNo = :target))
            AND (:keyword IS NULL OR (LOWER(b.bulletinTitleName) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(b.bulletinContent) LIKE CONCAT('%', LOWER(:keyword), '%')))
            ORDER BY b.createDt DESC
            """, countQuery = """
            SELECT COUNT(DISTINCT b)
            FROM BulletinBoard b
            LEFT JOIN BulletinBoardCustomerCompanyMapping m ON m.bulletinBoard = b
            LEFT JOIN m.customerCompany c
            WHERE b.isDeleted = false
            AND b.bulletinTypeCode = 'FAQ'
            AND (:category IS NULL OR (b.bulletinCategorySeparateCode = :category))
            AND (:keyword IS NULL OR (LOWER(b.bulletinTitleName) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(b.bulletinContent) LIKE CONCAT('%', LOWER(:keyword), '%')))
                        """)
    Page<BulletinBoard> searchFaq(
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable);

    Page<BulletinBoard> findByBulletinTypeCodeAndIsDeletedFalse(Specification<BulletinBoard> spec, Pageable page, String bulletinTypeCode);

}
