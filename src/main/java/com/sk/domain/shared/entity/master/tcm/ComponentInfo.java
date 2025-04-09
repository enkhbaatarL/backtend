package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tcm_cmpntinfom")
@Comment("TCM_컴포넌트정보기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentInfo extends BaseEntity {

    @Id
    @Column(name = "cmpnt_type_id", nullable = false, length = 30)
    @Comment("컴포넌트유형ID")
    private String componentTypeId;

    @Column(name = "cmpnt_type_nm", nullable = false, length = 100)
    @Comment("컴포넌트유형명")
    private String componentTypeNm;

    @Column(name = "gds_data_src_nm", nullable = false, length = 100)
    @Comment("상품데이터소스명")
    private String goodsDataSourceNm;

    @Column(name = "tgt_db_nm", nullable = false, length = 100)
    @Comment("대상데이터베이스명")
    private String targetDbNm;

    @Column(name = "tgt_tbl_nm", nullable = false, length = 50)
    @Comment("대상테이블명")
    private String targetTableNm;

    @Column(name = "qry_cdtn_cntt", length = 1000)
    @Comment("쿼리조건내용")
    private String queryConditionCntt;

    @Column(name = "tbl_colm_list_nm", length = 255)
    @Comment("테이블컬럼목록명")
    private String tableColumnListNm;

    @Column(name = "exl_colm_list_nm", length = 255)
    @Comment("엑셀컬럼목록명")
    private String excelColumnListNm;

    @Column(name = "exl_cdtn_cntt", length = 500)
    @Comment("엑셀조건내용")
    private String excelConditionCntt;

    @Builder
    public ComponentInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, String componentTypeId, String componentTypeNm, String goodsDataSourceNm,
            String targetDbNm, String targetTableNm, String queryConditionCntt,
            String tableColumnListNm, String excelColumnListNm, String excelConditionCntt) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.componentTypeId = componentTypeId;
        this.componentTypeNm = componentTypeNm;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.targetDbNm = targetDbNm;
        this.targetTableNm = targetTableNm;
        this.queryConditionCntt = queryConditionCntt;
        this.tableColumnListNm = tableColumnListNm;
        this.excelColumnListNm = excelColumnListNm;
        this.excelConditionCntt = excelConditionCntt;
    }

    //TODO: 호영: Request DTO 변경 필요
    public static ComponentInfo of(TestReq.saveComponent testReq) {
        return ComponentInfo.builder()
                .componentTypeId(testReq.componentTypeId())
                .componentTypeNm(testReq.componentTypeNm())
                .goodsDataSourceNm(testReq.goodsDataSourceNm())
                .targetDbNm(testReq.targetDbNm())
                .targetTableNm(testReq.targetTableNm())
                .queryConditionCntt(testReq.queryConditionCntt())
                .tableColumnListNm(testReq.tableColumnListNm())
                .excelColumnListNm(testReq.excelColumnListNm())
                .excelConditionCntt(testReq.excelConditionCntt())
                .build();
    }
}