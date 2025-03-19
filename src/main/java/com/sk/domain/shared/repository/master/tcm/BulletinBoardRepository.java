package com.sk.domain.shared.repository.master.tcm;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.bbs.BulletinBoardRes;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;

public interface BulletinBoardRepository extends JpaRepository<BulletinBoard, Integer> {

        // @Query(value = """
        // select new com.sk.domain.admin.dto.response.bbs.NoticeRes(

        // )
        // from BulletinBoard b
        // left join GoodsDataSource gds on g = gds.goods
        // where g.isDeleted = false
        // and g.provider.providerNo = :providerNo
        // and (
        // lower(g.goodsNm) like concat('%', lower(:keyword), '%') or
        // lower(g.goodsExpn) like concat('%', lower(:keyword), '%')
        // )
        // group by g.goodsNo, g.goodsNm, g.goodsExpn
        // order by g.goodsNm asc, g.goodsExpn asc
        // """, countQuery = """
        // select count(distinct g.goodsNo)
        // from Goods g
        // left join GoodsDataSource gds on g = gds.goods
        // where g.isDeleted = false
        // and g.provider.providerNo = :providerNo
        // and (
        // lower(g.goodsNm) like concat('%', lower(:keyword), '%') or
        // lower(g.goodsExpn) like concat('%', lower(:keyword), '%')
        // )

        // select distinct mapping.bulletinBoard
        // from BulletinBoardCustomerCompanyMapping mapping
        // join mapping.customerCompany cc
        // where (:bulletinTitle is null or mapping.bulletinBoard.bulletinTitleName like concat('%', :bulletinTitle, '%'))
        // and (:bulletinContent is null or mapping.bulletinBoard.bulletinContent like concat('%', :bulletinContent, '%'))
        // and (:customerCompanyNo is null or cc.customerCompanyNo = :customerCompanyNo)")

        // """)
        // Page<NoticeRes> findByProviderNoAndIsDeletedFalseWithGoodsDataSourceCount(@Param("keyword") String keyword, Pageable pageable);

        @Query(value = """
                        select distinct mapping.bulletinBoard
                        from BulletinBoardCustomerCompanyMapping mapping
                        join mapping.customerCompany cc
                        where (:bulletinTitle is null or mapping.bulletinBoard.bulletinTitleName like concat('%', :bulletinTitle, '%'))
                        and (:bulletinContent is null or mapping.bulletinBoard.bulletinContent like concat('%', :bulletinContent, '%'))
                        and (:customerCompanyNo is null or cc.customerCompanyNo = :customerCompanyNo)
                        """, countQuery = """
                        select count(distinct mapping.bulletinBoard)
                        from BulletinBoardCustomerCompanyMapping mapping
                        join mapping.customerCompany cc
                        where (:bulletinTitle is null or mapping.bulletinBoard.bulletinTitleName like concat('%', :bulletinTitle, '%'))
                        and (:bulletinContent is null or mapping.bulletinBoard.bulletinContent like concat('%', :bulletinContent, '%'))
                        and (:customerCompanyNo is null or cc.customerCompanyNo = :customerCompanyNo)
                        """)
        Page<BulletinBoard> searchBulletinBoards(
                        @Param("bulletinTitle") String bulletinTitle,
                        @Param("bulletinContent") String bulletinContent,
                        @Param("customerCompanyNo") Integer customerCompanyNo,
                        Pageable pageable);

        @Query(value = """
                        select b
                        from BulletinBoard
                        where b.isDeleted = false
                        and b.bulletinNo = :bulletinNo
                                                """)
        Optional<BulletinBoard> findByBulletinNo(@Param("bulletinNo") Integer bulletinNo);
}
