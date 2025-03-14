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

  List<Provider> findAllByIsDeletedFalse();

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
      order by p.createDt asc
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
      cu.userNm,
      p.modifyUserId,
      p.modifyDt,
      mu.userNm
      )
      from Provider p
      left join User cu on p.createUserId = cu.userLoginId
      left join User mu on p.modifyUserId = mu.userLoginId
      where p.isDeleted = false and p.providerNo = :providerNo
      """)
  Optional<ProviderRes.Get> findByProviderNoWithUser(@Param("providerNo") int providerNo);

  Optional<Provider> findByProviderNoAndIsDeletedFalse(int providerNo);

  @Query(value = """
      select new com.sk.domain.admin.dto.response.ProviderRes$ModifyDt(
      max(case when p.modifyDt > g.modifyDt then p.modifyDt else g.modifyDt end)
      )
      from Provider p
      left join Goods g on p = g.provider
      where p.isDeleted = false
      and p.providerNo = :providerNo
      """)
  Optional<ProviderRes.ModifyDt> findByProviderNoWithGoodsLatestModifyDt(int providerNo);
}
