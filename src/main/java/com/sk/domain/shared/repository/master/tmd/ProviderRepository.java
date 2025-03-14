package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.ProviderRes;
import com.sk.domain.shared.entity.master.tmd.Provider;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {

  Optional<Provider> findByProviderNoAndIsDeletedFalse(int providerNo);

  @Query(value = """
      select new com.sk.domain.admin.dto.response.ProviderRes$Name(
      p.providerNo,
      p.providerNm
        )
      from Provider p
      where p.isDeleted = false
      order by p.providerNm asc
            """)
  List<ProviderRes.Name> findAllByIsDeletedFalseOrderByCreateDtDesc();

  @Query(value = """
      select new com.sk.domain.admin.dto.response.ProviderRes(
      p.providerNo,
      p.providerNm,
      p.providerCd,
      p.isUsed,
      count(g),
      p.createDt
      )
      from Provider p
      left join Goods g on p = g.provider
      where p.isDeleted = false
      group by p.providerNo, p.providerNm, p.providerCd, p.isUsed, p.createDt
      order by p.providerNm asc
      """, countQuery = """
      select count(distinct p.providerNo)
      from Provider p
      where p.isDeleted = false
      """)
  Page<ProviderRes> findAllWithGoodsCount(Pageable pageable);

  @Query(value = """
      select new com.sk.domain.admin.dto.response.ProviderRes$Get(
      p.providerNo,
      p.providerNm,
      p.providerCd,
      p.providerExpn,
      p.isUsed,
      p.createUserId,
      p.createDt,
      cu.userNm
      )
      from Provider p
      left join User cu on p.createUserId = cu.userLoginId
      where p.isDeleted = false
      and p.providerNo = :providerNo
      """)
  Optional<ProviderRes.Get> findByProviderNoWithUser(@Param("providerNo") int providerNo);

  // 프로바이더의 수정시간이 상품의 수정시간보다 최근이면 프로바이더의 수정시간과 수정자를 가져오고
  // 상품의 수정시간이 프로바이더의 수정시간보다 최근이면 상품의 수정시간과 수정자를 가져온다.
  @Query(value = """
      select new com.sk.domain.admin.dto.response.ProviderRes$LatestUpdated(
      CASE WHEN p.modifyDt >= COALESCE((SELECT MAX(g.modifyDt) FROM Goods g WHERE g.provider.providerNo = :providerNo), p.modifyDt) THEN p.modifyDt ELSE (SELECT MAX(g.modifyDt) FROM Goods g WHERE g.provider.providerNo = :providerNo) END,
      CASE WHEN p.modifyDt >= COALESCE((SELECT MAX(g.modifyDt) FROM Goods g WHERE g.provider.providerNo = :providerNo), p.modifyDt) THEN (SELECT u.userNm FROM User u WHERE u.userLoginId = p.modifyUserId) ELSE (SELECT u.userNm FROM User u WHERE u.userLoginId = (SELECT g.modifyUserId FROM Goods g WHERE g.modifyDt = (SELECT MAX(g2.modifyDt) FROM Goods g2 WHERE g2.provider.providerNo = :providerNo))) END
      )
      from Provider p
      where p.isDeleted = false
      and p.providerNo = :providerNo
      """)
  Optional<ProviderRes.LatestUpdated> findByProviderNoWithGoodsLatestModifyInfo(int providerNo);
}
