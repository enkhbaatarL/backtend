package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

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
    private String componentTypeName;

    @Column(name = "gds_data_src_nm", nullable = false, length = 100)
    @Comment("상품데이터소스명")
    private String goodsDataSourceName;

    @Column(name = "tgt_db_nm", nullable = false, length = 100)
    @Comment("대상데이터베이스명")
    private String targetDbName;

    @Column(name = "tgt_tbl_nm", nullable = false, length = 50)
    @Comment("대상테이블명")
    private String targetTableName;

    @Column(name = "qry_cdtn_cntt", length = 1000)
    @Comment("쿼리조건내용")
    private String queryConditionContent;

    @Column(name = "tbl_colm_list_nm", length = 255)
    @Comment("테이블컬럼목록명")
    private String tableColumnListName;

    @Column(name = "exl_colm_list_nm", length = 255)
    @Comment("엑셀컬럼목록명")
    private String excelColumnListName;

    @Column(name = "exl_cdtn_cntt", length = 500)
    @Comment("엑셀조건내용")
    private String excelConditionContent;

    @Builder
    public ComponentInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, String componentTypeId, String componentTypeName, String goodsDataSourceName,
            String targetDbName, String targetTableName, String queryConditionContent,
            String tableColumnListName, String excelColumnListName, String excelConditionContent) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.componentTypeId = componentTypeId;
        this.componentTypeName = componentTypeName;
        this.goodsDataSourceName = goodsDataSourceName;
        this.targetDbName = targetDbName;
        this.targetTableName = targetTableName;
        this.queryConditionContent = queryConditionContent;
        this.tableColumnListName = tableColumnListName;
        this.excelColumnListName = excelColumnListName;
        this.excelConditionContent = excelConditionContent;
    }
}