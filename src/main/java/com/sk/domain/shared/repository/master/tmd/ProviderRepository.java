package com.sk.domain.shared.repository.master.tmd;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sk.domain.admin.dto.response.ProviderRes;
import com.sk.domain.shared.entity.master.tmd.Provider;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {

  @Query(value = """
      select new com.sk.domain.admin.dto.response.ProviderRes$All(
      p.providerNo,
      p.providerNm,
      p.providerCd,
      p.providerExpn,
      p.isUsed,
      count(g),
      p.createUserId,
      p.createDt,
      p.modifyUserId,
      p.modifyDt
      )
      from Provider p
      left join Goods g on p = g.provider
      where p.isDeleted = false
      group by p.providerNo, p.providerNm, p.providerCd, p.providerExpn, p.isUsed, p.createUserId, p.createDt, p.modifyUserId, p.modifyDt
      order by p.createDt asc
      """, countQuery = "select count(p) from Provider p")
  Page<ProviderRes.All> findAllWithGoodsCount(Pageable pageable);

  @Query(value = """
      select new com.sk.domain.admin.dto.response.ProviderRes$One(
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
  Optional<ProviderRes.One> findByProviderNoWithUser(int providerNo);

  Optional<Provider> findByProviderNoAndIsDeletedFalse(int providerNo);
}
