package com.sk.domain.shared.repository.master.tmd;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.admin.dto.response.GoodsRes;

public interface GoodsRepository extends JpaRepository<Goods, Integer> {
  @Query(value = """
      select new com.sk.domain.admin.dto.response.GoodsRes$All(
      g.goodsNo,
      g.goodsNm,
      g.goodsCd,
      g.goodsExpn,
      g.isUsed,
      g.createUserId,
      g.createDt,
      g.modifyUserId,
      g.modifyDt
        )
      from Goods g
      where g.isDeleted = false
      and g.provider.providerNo = :providerNo
      and (g.goodsNm like concat('%', :keyword, '%') or g.goodsCd like concat('%', :keyword, '%') or g.goodsExpn like concat('%', :keyword, '%'))
            """)
  Page<GoodsRes.All> findByProviderNoAndIsDeletedFalse(@Param("providerNo") int providerNo,
      @Param("keyword") String keyword, Pageable pageable);

  @Query(value = """
      select new com.sk.domain.admin.dto.response.GoodsRes$One(
      g.goodsNo,
      g.goodsNm,
      g.goodsCd,
      g.goodsExpn,
      g.isUsed,
      g.createUserId,
      g.createDt,
      cu.userNm,
      g.modifyUserId,
      g.modifyDt,
      mu.userNm
      )
      from Goods g
      left join User cu on g.createUserId = cu.userLoginId
      left join User mu on g.modifyUserId = mu.userLoginId
      where g.isDeleted = false and g.goodsNo = :goodsNo
      """)
  Optional<GoodsRes.One> findByGoodsNoWithUser(@Param("goodsNo") int goodsNo);

  Optional<Goods> findByGoodsNoAndIsDeletedFalse(int goodsNo);
}
