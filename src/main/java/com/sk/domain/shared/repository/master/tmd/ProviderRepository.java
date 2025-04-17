package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.shared.entity.master.tmd.Provider;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    /**
     * 
     * 기본적으로 고객사나 사용자의 사용가능 상태는 체크 하지 않는다. 로그인 단에서 체크하는걸로 처리
     *
     */

    Optional<Provider> findByProviderNoAndIsDeletedFalse(int providerNo);

    Optional<Provider> findByProviderNoAndIsDeletedFalseAndIsUsedTrue(int providerNo);

    Optional<Provider> findByProviderIdAndIsDeletedFalse(String providerId);

    // 관리자는 전체 프로바이더 목록을 조회한다.
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.ProviderRes(
                p.providerNo,
                p.providerNm,
                p.providerId,
                p.isUsed,
                COUNT(g),
                p.createDt
            )
            FROM Provider p
            LEFT JOIN Goods g ON p = g.provider AND g.isDeleted = false
            WHERE p.isDeleted = false
            GROUP BY p.providerNo, p.providerNm, p.providerId, p.isUsed, p.createDt
            ORDER BY p.providerNm ASC
            """, countQuery = """
            SELECT COUNT(DISTINCT p.providerNo)
            FROM Provider p
            WHERE p.isDeleted = false
            """)
    Page<ProviderRes> findAllForAdmin(Pageable pageable);

    // 운영자는 고객사에 등록된 프로바이더 목록을 조회한다.
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.ProviderRes(
                p.providerNo,
                p.providerNm,
                p.providerId,
                p.isUsed,
                COUNT(g),
                p.createDt
            )
            FROM Provider p
            LEFT JOIN Goods g ON p = g.provider AND g.isDeleted = false
            JOIN CustomerCompanyProviderMapping cp ON p = cp.provider
            WHERE p.isDeleted = false
                AND p.isUsed = true
                AND cp.isDeleted = false
                AND cp.isUsed = true
                AND cp.customerCompany.customerCompanyNo = :customerCompanyNo
            GROUP BY p.providerNo, p.providerNm, p.providerId, p.isUsed, p.createDt
            ORDER BY p.providerNm ASC
            """, countQuery = """
            SELECT COUNT(DISTINCT p.providerNo)
            FROM Provider p
            JOIN CustomerCompanyProviderMapping cp ON p = cp.provider
            WHERE p.isDeleted = false
                AND cp.customerCompany.customerCompanyNo = :customerCompanyNo
            """)
    Page<ProviderRes> findAllForOperator(@Param("customerCompanyNo") int customerCompanyNo, Pageable pageable);

    // 관리자는 전체 프로바이더 목록을 조회한다.
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.ProviderNameRes(
                p.providerNo,
                p.providerNm,
                p.isUsed
            )
            FROM Provider p
            WHERE p.isDeleted = false
            ORDER BY p.providerNm ASC
                  """)
    List<ProviderNameRes> findProviderNmForAdmin();

    // 운영자는 고객사에 등록된 프로바이더 목록을 조회한다.
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.ProviderNameRes(
                p.providerNo,
                p.providerNm,
                p.isUsed
            )
            FROM Provider p
            JOIN CustomerCompanyProviderMapping cp ON p = cp.provider
            WHERE p.isDeleted = false
                AND p.isUsed = true
                AND cp.isDeleted = false
                AND cp.isUsed = true
                AND cp.customerCompany.customerCompanyNo = :customerCompanyNo
            ORDER BY p.providerNm ASC
                  """)
    List<ProviderNameRes> findProviderNmForOperator(@Param("customerCompanyNo") int customerCompanyNo);

    // 상세보기
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.ProviderDetailRes(
                p.providerNo,
                p.providerNm,
                p.providerId,
                p.providerExpn,
                p.isUsed,
                new com.sk.domain.shared.dto.AuditInfo(
                          p.createUserId,
                          p.createDt,
                          p.modifyUserId,
                          p.modifyDt,
                          new com.sk.domain.shared.dto.AuditExtension(
                              cu.userNm,
                              cu.departmentNm,
                              cuc.customerCompanyNm,
                              mu.userNm,
                              mu.departmentNm,
                              muc.customerCompanyNm
                          )
                      )
            )
            FROM Provider p
            LEFT JOIN User cu ON p.createUserId = cu.userLoginId AND cu.userRoleCd = 'ADMIN'
            LEFT JOIN cu.customerCompany cuc
            LEFT JOIN User mu ON p.modifyUserId = mu.userLoginId AND mu.userRoleCd = 'ADMIN'
            LEFT JOIN mu.customerCompany muc
            WHERE p.isDeleted = false
                AND p.providerNo = :providerNo
            GROUP BY p.providerNo, p.providerNm, p.providerId, p.providerExpn, p.isUsed,
                     cu.userNm, cu.departmentNm, cuc.customerCompanyNm,
                     mu.userNm, mu.departmentNm, muc.customerCompanyNm
            """)
    Optional<ProviderDetailRes> findProviderDetailByProviderNo(@Param("providerNo") int providerNo);

    // 프로바이더의 수정시간이 상품의 수정시간보다 최근이면 프로바이더의 수정시간과 수정자를 가져오고
    // 상품의 수정시간이 프로바이더의 수정시간보다 최근이면 상품의 수정시간과 수정자를 가져온다.
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes(
                CASE
                    WHEN p.modifyDt >= COALESCE((SELECT MAX(g.modifyDt) FROM Goods g WHERE g.provider.providerNo = :providerNo), p.modifyDt)
                    THEN p.modifyDt
                    ELSE (SELECT MAX(g.modifyDt) FROM Goods g WHERE g.provider.providerNo = :providerNo)
                    END,
                CASE
                    WHEN p.modifyDt >= COALESCE((SELECT MAX(g.modifyDt) FROM Goods g WHERE g.provider.providerNo = :providerNo), p.modifyDt)
                    THEN (SELECT u.userNm FROM User u WHERE u.userLoginId = p.modifyUserId AND (u.userRoleCd = 'ADMIN' OR u.userRoleCd = 'OPERATOR'))
                    ELSE (SELECT u.userNm FROM User u WHERE (u.userRoleCd = 'ADMIN' OR u.userRoleCd = 'OPERATOR') AND u.userLoginId = (SELECT g.modifyUserId FROM Goods g WHERE g.modifyDt = (SELECT MAX(g2.modifyDt) FROM Goods g2 WHERE g2.provider.providerNo = :providerNo)))
                    END
            )
            FROM Provider p
            WHERE p.isDeleted = false
                AND p.providerNo = :providerNo
            """)
    Optional<ProviderLatestUpdatedRes> findProviderLatestUpdatedByProviderNo(@Param("providerNo") int providerNo);
}
