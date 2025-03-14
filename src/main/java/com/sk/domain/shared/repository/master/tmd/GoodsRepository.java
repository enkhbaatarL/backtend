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
      select new com.sk.domain.admin.dto.response.GoodsRes(
      g.goodsNo,
      g.goodsNm,
      g.goodsExpn,
      count(gds)
        )
      from Goods g
      left join GoodsDataSource gds on g = gds.goods
      where g.isDeleted = false
      and g.provider.providerNo = :providerNo
      and (
          lower(g.goodsNm) like concat('%', lower(:keyword), '%') or
          lower(g.goodsExpn) like concat('%', lower(:keyword), '%')
      )
      group by g.goodsNo, g.goodsNm, g.goodsExpn
      order by g.goodsNm asc, g.goodsExpn asc
            """, countQuery = """
      select count(distinct g.goodsNo)
      from Goods g
      left join GoodsDataSource gds on g = gds.goods
      where g.isDeleted = false
      and g.provider.providerNo = :providerNo
      and (
          lower(g.goodsNm) like concat('%', lower(:keyword), '%') or
          lower(g.goodsExpn) like concat('%', lower(:keyword), '%')
      )
      """)
  Page<GoodsRes> findByProviderNoAndIsDeletedFalse(@Param("providerNo") int providerNo,
      @Param("keyword") String keyword, Pageable pageable);

  @Query(value = """
      select new com.sk.domain.admin.dto.response.GoodsRes$Get(
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
      where g.isDeleted = false
      and g.goodsNo = :goodsNo
      """)
  Optional<GoodsRes.Get> findByGoodsNoWithUser(@Param("goodsNo") int goodsNo);

  Optional<Goods> findByGoodsNoAndIsDeletedFalse(int goodsNo);
}
