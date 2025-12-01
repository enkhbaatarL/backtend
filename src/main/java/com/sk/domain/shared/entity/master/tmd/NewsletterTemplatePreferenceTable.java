package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;


@Entity
@Table(name = "tmd_nws_ltr_tmplt_pref_tbl",
    indexes = {
        @Index(name = "tmd_nws_ltr_tmplt_pref_tbl_ix01", columnList = "pref_tbl_cd")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("뉴스레터 템플릿 참조 테이블")
public class NewsletterTemplatePreferenceTable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_reg_sn", columnDefinition = "int unsigned")
    @Comment("데이터등록일련번호")
    private Integer dataRegSn;

    @Column(name = "pref_tbl_cd", nullable = false, length = 50)
    @Comment("참조 테이블 코드")
    private String preferenceTableCode;

    @Column(name = "tgt_db_nm", nullable = false, length = 100)
    @Comment("대상데이터베이스명")
    private String targetDatabaseName;

    @Column(name = "tgt_tbl_nm", nullable = false, length = 50)
    @Comment("대상테이블명")
    private String targetTableName;

    @Column(name = "qry_cdtn_cntt", length = 1000)
    @Comment("쿼리조건내용")
    private String queryConditionContent;

    @Column(name = "tbl_colm_nm", length = 50)
    @Comment("테이블컬럼목록명")
    private String tableColumnName;

    @Builder
    public NewsletterTemplatePreferenceTable(String preferenceTableCode, String targetDatabaseName,
        String targetTableName, String queryConditionContent,
        String tableColumnName) {
        this.preferenceTableCode = preferenceTableCode;
        this.targetDatabaseName = targetDatabaseName;
        this.targetTableName = targetTableName;
        this.queryConditionContent = queryConditionContent;
        this.tableColumnName = tableColumnName;
    }

}
