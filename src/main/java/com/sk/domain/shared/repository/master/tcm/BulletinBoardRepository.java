package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tcm.BulletinBoard;

import com.sk.domain.admin.dto.response.bbs.NoticeRes;
import com.sk.domain.admin.dto.response.bbs.FaqRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeRes;
import com.sk.domain.mi.dto.response.bbs.MiFaqRes;

public interface BulletinBoardRepository extends JpaRepository<BulletinBoard, Integer> {

    /*******************************************************************
    *
    * 공통
    *
    *******************************************************************/
    Optional<BulletinBoard> findByBulletinNoAndBulletinTypeCdAndIsDeletedFalse(Integer bulletinNo, String bulletinTypeCode);

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
                b.bulletinCategorySeparateCd,
                b.bulletinTitleNm,
                CASE
                    WHEN b.bulletinValidBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                         AND b.bulletinValidEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                    THEN 'ACTIVE'
                    ELSE 'INACTIVE'
                END,
                COALESCE(CONCAT(FUNCTION('GROUP_CONCAT', c.customerCompanyNm), ''), '전체'),
                new com.sk.domain.shared.dto.AuditInfo(
                    b.createUserId,
                    b.createDt,
                    b.modifyUserId,
                    b.modifyDt,
                    new com.sk.domain.shared.dto.AuditExtension(
                        COALESCE(cu.userNm, null),
                        COALESCE(cu.departmentNm, null),
                        COALESCE(cuc.customerCompanyNm, null),
                        COALESCE(mu.userNm, null),
                        COALESCE(mu.departmentNm, null),
                        COALESCE(muc.customerCompanyNm, null)
                    )
                )
            )
            FROM BulletinBoard b
            LEFT JOIN BulletinBoardCustomerCompanyMapping m ON m.bulletinBoard = b
            LEFT JOIN m.customerCompany c
            LEFT JOIN User cu ON b.createUserId = cu.userLoginId AND cu.userRoleCd = 'ADMIN'
            LEFT JOIN cu.customerCompany cuc
            LEFT JOIN User mu ON b.modifyUserId = mu.userLoginId AND mu.userRoleCd = 'ADMIN'
            LEFT JOIN mu.customerCompany muc
            WHERE b.isDeleted = false
              AND b.bulletinTypeCd = 'NOTICE'
              AND (:category IS NULL OR b.bulletinCategorySeparateCd = :category)
              AND (:status IS NULL OR (
                  (:status = 'ACTIVE' AND b.bulletinValidBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                   AND b.bulletinValidEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d'))
                  OR (:status = 'INACTIVE' AND (b.bulletinValidBeginDt > FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                   OR b.bulletinValidEndDt < FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')))
              ))
              AND (:target IS NULL OR c.customerCompanyNo = :target)
              AND (:keyword IS NULL OR (
                  LOWER(b.bulletinTitleNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(b.bulletinCntt) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            GROUP BY
                b.bulletinNo,
                b.isTopFixed,
                b.isPopupUse,
                b.bulletinCategorySeparateCd,
                b.bulletinTitleNm
            ORDER BY b.createDt DESC
            """, countQuery = """
            SELECT COUNT(DISTINCT b.bulletinNo)
            FROM BulletinBoard b
            LEFT JOIN BulletinBoardCustomerCompanyMapping m ON m.bulletinBoard = b
            LEFT JOIN m.customerCompany c
            WHERE b.isDeleted = false
            AND b.bulletinTypeCd = 'NOTICE'
            AND (:category IS NULL OR b.bulletinCategorySeparateCd = :category)
            AND (:status IS NULL OR (
                (:status = 'ACTIVE' AND b.bulletinValidBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND b.bulletinValidEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d'))
                OR (:status = 'INACTIVE' AND (b.bulletinValidBeginDt > FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                OR b.bulletinValidEndDt < FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')))
            ))
            AND (:target IS NULL OR c.customerCompanyNo = :target)
            AND (:keyword IS NULL OR (
                LOWER(b.bulletinTitleNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                OR LOWER(b.bulletinCntt) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            """)
    Page<NoticeRes> findAllNoticeForAdminPage(
            @Param("category") String category,
            @Param("status") String status,
            @Param("target") Integer target,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            SELECT new com.sk.domain.mi.dto.response.bbs.MiNoticeRes(
                b.bulletinNo,
                b.isTopFixed,
                b.bulletinCategorySeparateCd,
                b.bulletinTitleNm,
                b.createDt,
                COALESCE(u.isRead, false)
            )
            FROM BulletinBoard b
            LEFT JOIN BulletinBoardCustomerCompanyMapping m ON m.bulletinBoard = b
            LEFT JOIN BulletinBoardNotificationUser u ON u.bulletinBoard = b and u.user.userNo = :userNo
            LEFT JOIN m.customerCompany c
            WHERE b.isDeleted = false
              AND b.bulletinTypeCd = 'NOTICE'
              AND b.bulletinValidBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
              AND b.bulletinValidEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
              AND (b.isWholePost = 'Y' OR (b.isWholePost = 'N' AND c.customerCompanyNo = :customerCompanyNo))
              AND (:category IS NULL OR b.bulletinCategorySeparateCd = :category)
              AND (:keyword IS NULL OR (
                  LOWER(b.bulletinTitleNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(b.bulletinCntt) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            GROUP BY
                b.bulletinNo,
                b.isTopFixed,
                b.bulletinCategorySeparateCd,
                b.bulletinTitleNm
            ORDER BY b.isTopFixed DESC, b.createDt DESC
            """, countQuery = """
            SELECT COUNT(DISTINCT b.bulletinNo)
            FROM BulletinBoard b
            LEFT JOIN BulletinBoardCustomerCompanyMapping m ON m.bulletinBoard = b
            LEFT JOIN m.customerCompany c
            WHERE b.isDeleted = false
            AND b.bulletinTypeCd = 'NOTICE'
              AND b.bulletinValidBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
              AND b.bulletinValidEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
              AND (b.isWholePost = 'Y' OR (b.isWholePost = 'N' AND c.customerCompanyNo = :customerCompanyNo))
              AND (:category IS NULL OR b.bulletinCategorySeparateCd = :category)
              AND (:keyword IS NULL OR (
                  LOWER(b.bulletinTitleNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(b.bulletinCntt) LIKE CONCAT('%', LOWER(:keyword), '%')
              ))
            """)
    Page<MiNoticeRes> findAllNoticeForServicePage(
            @Param("customerCompanyNo") int customerCompanyNo,
            @Param("userNo") int userNo,
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            SELECT b
            FROM BulletinBoard b
            WHERE b.isDeleted = false
            AND b.isPopupUse = true
            AND (b.popupPostBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
            AND b.popupPostEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
            )
            """)
    Optional<BulletinBoard> findByIsPopupUseTrue();

    @Query("""
            UPDATE BulletinBoard b
            SET b.isPopupUse = false
            WHERE b.bulletinTypeCd = 'NOTICE'
            """)
    void updateAllPopupFalse();

    /*******************************************************************
    *
    * FAQ
    *
    *******************************************************************/
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.bbs.FaqRes(
                b.bulletinNo,
                b.bulletinCategorySeparateCd,
                b.bulletinTitleNm,
                new com.sk.domain.shared.dto.AuditInfo(
                    b.createUserId,
                    b.createDt,
                    b.modifyUserId,
                    b.modifyDt,
                    new com.sk.domain.shared.dto.AuditExtension(
                        COALESCE(cu.userNm, null),
                        COALESCE(cu.departmentNm, null),
                        COALESCE(cuc.customerCompanyNm, null),
                        COALESCE(mu.userNm, null),
                        COALESCE(mu.departmentNm, null),
                        COALESCE(muc.customerCompanyNm, null)
                    )
                )
            )
            FROM BulletinBoard b
            LEFT JOIN User cu ON b.createUserId = cu.userLoginId AND cu.userRoleCd = 'ADMIN'
            LEFT JOIN cu.customerCompany cuc
            LEFT JOIN User mu ON b.modifyUserId = mu.userLoginId AND mu.userRoleCd = 'ADMIN'
            LEFT JOIN mu.customerCompany muc
            WHERE b.isDeleted = false
            AND b.bulletinTypeCd = 'FAQ'
            AND (:category IS NULL OR (b.bulletinCategorySeparateCd = :category))
            AND (:keyword IS NULL OR (LOWER(b.bulletinTitleNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(b.bulletinCntt) LIKE CONCAT('%', LOWER(:keyword), '%')))
            ORDER BY b.createDt ASC
            """, countQuery = """
            SELECT COUNT(DISTINCT b)
            FROM BulletinBoard b
            LEFT JOIN User cu ON b.createUserId = cu.userLoginId
            LEFT JOIN cu.customerCompany cuc
            WHERE b.isDeleted = false
            AND b.bulletinTypeCd = 'FAQ'
            AND (:category IS NULL OR (b.bulletinCategorySeparateCd = :category))
            AND (:keyword IS NULL OR (LOWER(b.bulletinTitleNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(b.bulletinCntt) LIKE CONCAT('%', LOWER(:keyword), '%')))
                        """)
    Page<FaqRes> findAllFaqForAdminPage(
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            SELECT new com.sk.domain.mi.dto.response.bbs.MiFaqRes(
                b.bulletinNo,
                b.bulletinTitleNm,
                b.bulletinCntt
            )
            FROM BulletinBoard b
            WHERE b.isDeleted = false
            AND b.bulletinTypeCd = 'FAQ'
            AND (:category IS NULL OR (b.bulletinCategorySeparateCd = :category))
            AND (:keyword IS NULL OR (LOWER(b.bulletinTitleNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(b.bulletinCntt) LIKE CONCAT('%', LOWER(:keyword), '%')))
            ORDER BY b.createDt ASC
            """, countQuery = """
            SELECT COUNT(DISTINCT b)
            FROM BulletinBoard b
            WHERE b.isDeleted = false
            AND b.bulletinTypeCd = 'FAQ'
            AND (:category IS NULL OR (b.bulletinCategorySeparateCd = :category))
            AND (:keyword IS NULL OR (LOWER(b.bulletinTitleNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(b.bulletinCntt) LIKE CONCAT('%', LOWER(:keyword), '%')))
                        """)
    Page<MiFaqRes> findAllFaqForServicePage(
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable);

}
