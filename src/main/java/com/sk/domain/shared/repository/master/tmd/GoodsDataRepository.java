package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.GoodsDataDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataNameRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsData;

public interface GoodsDataRepository extends JpaRepository<GoodsData, Integer> {

    // todo: Type 작업 해야 합
    // 데이타는 무조건 파일을 올리게 되어 있으니 파일 유무를 체크해서 all, 정형, 비정형 판단해도 되나?
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataRes(
                gd.goodsDataNo,
                gd.goodsDataNm,
                gd.goodsDataExpn
            )
            FROM GoodsData gd
            WHERE gd.isDeleted = false
              AND gd.goods.provider.providerNo = :providerNo
              AND gd.goods.goodsNo = :goodsNo
              AND (:keyword IS NULL OR (LOWER(gd.goodsDataNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(gd.goodsDataExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            ORDER BY gd.goodsDataNm ASC
            """, countQuery = """
            SELECT COUNT(DISTINCT gd.goodsDataNo)
            FROM GoodsData gd
            WHERE gd.isDeleted = false
              AND gd.goods.provider.providerNo = :providerNo
              AND gd.goods.goodsNo = :goodsNo
              AND (:keyword IS NULL OR (LOWER(gd.goodsDataNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(gd.goodsDataExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            """)
    Page<GoodsDataRes> searchGoodsData(@Param("providerNo") Integer providerNo, @Param("goodsNo") Integer goodsNo,
            @Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataNameRes(
            gd.goodsDataNo,
            gd.goodsDataNm
            )
            FROM GoodsData gd
            WHERE gd.isDeleted = false
            AND gd.goods.provider.providerNo = :providerNo
            AND gd.goods.goodsNo = :goodsNo
                        """)
    List<GoodsDataNameRes> getAllGoodsDataNm(@Param("providerNo") Integer providerNo, @Param("goodsNo") Integer goodsNo);

    @Query(value = """
            SELECT gd
            FROM GoodsData gd
            WHERE gd.isDeleted = false
              AND gd.goods.provider.providerNo = :providerNo
              AND gd.goods.goodsNo = :goodsNo
              AND gd.goodsDataNo = :goodsDataNo
            """)
    Optional<GoodsData> getGoodsData(@Param("providerNo") Integer providerNo, @Param("goodsNo") Integer goodsNo,
            @Param("goodsDataNo") Integer goodsDataNo);
}
