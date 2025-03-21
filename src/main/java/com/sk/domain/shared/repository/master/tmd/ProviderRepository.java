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

  List<Provider> findByIsDeletedFalseOrderByProviderNmAsc();

  @Query(value = """
      SELECT new com.sk.domain.admin.dto.response.provider.ProviderNameRes(
      p.providerNo,
      p.providerNm
        )
      FROM Provider p
      WHERE p.isDeleted = false
      ORDER BY p.providerNm ASC
            """)
  List<ProviderNameRes> findNameByIsDeletedFalseOrderByProviderNmAsc();

  @Query(value = """
      SELECT new com.sk.domain.admin.dto.response.provider.ProviderRes(
      p.providerNo,
      p.providerNm,
      p.providerCd,
      p.isUsed,
      count(g),
      p.createDt
      )
      FROM Provider p
      LEFT JOIN Goods g on p = g.provider
      WHERE p.isDeleted = false
      GROUP BY p.providerNo, p.providerNm, p.providerCd, p.isUsed, p.createDt
      ORDER BY p.providerNm ASC
      """, countQuery = """
      SELECT COUNT(distinct p.providerNo)
      FROM Provider p
      WHERE p.isDeleted = false
      """)
  Page<ProviderRes> searchProviders(Pageable pageable);

  // 상세보기
  @Query(value = """
      SELECT new com.sk.domain.admin.dto.response.provider.ProviderDetailRes(
      p.providerNo,
      p.providerNm,
      p.providerCd,
      p.providerExpn,
      p.isUsed,
      p.createUserId,
      p.createDt,
      cu.userNm
      )
      FROM Provider p
      LEFT JOIN User cu on p.createUserId = cu.userLoginId
      WHERE p.isDeleted = false
      AND p.providerNo = :providerNo
      """)
  Optional<ProviderDetailRes> findByProviderNoAndIsDeletedFalseWithUser(@Param("providerNo") Integer providerNo);

  Optional<Provider> findByProviderNoAndIsDeletedFalse(Integer providerNo);

  // 프로바이더의 수정시간이 상품의 수정시간보다 최근이면 프로바이더의 수정시간과 수정자를 가져오고
  // 상품의 수정시간이 프로바이더의 수정시간보다 최근이면 상품의 수정시간과 수정자를 가져온다.
  @Query(value = """
      SELECT new com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes(
      CASE WHEN p.modifyDt >= COALESCE((SELECT MAX(g.modifyDt) FROM Goods g WHERE g.provider.providerNo = :providerNo), p.modifyDt) THEN p.modifyDt ELSE (SELECT MAX(g.modifyDt) FROM Goods g WHERE g.provider.providerNo = :providerNo) END,
      CASE WHEN p.modifyDt >= COALESCE((SELECT MAX(g.modifyDt) FROM Goods g WHERE g.provider.providerNo = :providerNo), p.modifyDt) THEN (SELECT u.userNm FROM User u WHERE u.userLoginId = p.modifyUserId) ELSE (SELECT u.userNm FROM User u WHERE u.userLoginId = (SELECT g.modifyUserId FROM Goods g WHERE g.modifyDt = (SELECT MAX(g2.modifyDt) FROM Goods g2 WHERE g2.provider.providerNo = :providerNo))) END
      )
      FROM Provider p
      WHERE p.isDeleted = false
      AND p.providerNo = :providerNo
      """)
  Optional<ProviderLatestUpdatedRes> findByProviderNoAndIsDeletedFalseWithGoodsLatestModifyInfo(Integer providerNo);
}
