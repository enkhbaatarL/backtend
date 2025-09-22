package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 뉴스레터 템플릿 수집 차트 엔티티
 */
@Entity
@Table(name = "tmd_nws_ltr_tmplt_cltn_chrt",
       indexes = {
           @Index(name = "tmd_nws_ltr_tmplt_cltn_chrt_ix01", columnList = "cltn_chrt_type")
       })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("뉴스레터 템플릿 수집 차트")
public class NewsletterTemplateCollectionChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_reg_sn")
    @Comment("데이터등록일련번호")
    private Integer dataRegSn;

    @Column(name = "crt_usr_logn_id", nullable = false, length = 50)
    @Comment("생성사용자로그인ID")
    private String createUserLoginId = "SYSTEM";

    @Column(name = "crt_dtm", nullable = false)
    @Comment("생성일시")
    private LocalDateTime createDateTime;

    @Column(name = "mod_usr_logn_id", nullable = false, length = 50)
    @Comment("수정사용자로그인ID")
    private String modifyUserLoginId = "SYSTEM";

    @Column(name = "mod_dtm", nullable = false)
    @Comment("수정일시")
    private LocalDateTime modifyDateTime;

    @Column(name = "del_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("삭제여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private boolean isDeleted = false;

    @Column(name = "cltn_chrt_type", nullable = false, length = 50)
    @Comment("수집차트 유형")
    private String collectionChartType;

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

    @Column(name = "color", length = 20)
    @Comment("색상")
    private String color;


    /**
     * 생성자
     */
    public NewsletterTemplateCollectionChart(String collectionChartType, String targetDatabaseName,
                                           String targetTableName, String queryConditionContent,
                                           String tableColumnName, String color) {
        this.collectionChartType = collectionChartType;
        this.targetDatabaseName = targetDatabaseName;
        this.targetTableName = targetTableName;
        this.queryConditionContent = queryConditionContent;
        this.tableColumnName = tableColumnName;
        this.color = color;
    }

    /**
     * 정적 팩토리 메서드 - 필수 필드만
     */
    public static NewsletterTemplateCollectionChart of(String collectionChartType, String targetDatabaseName, String targetTableName) {
        return new NewsletterTemplateCollectionChart(collectionChartType, targetDatabaseName, targetTableName, null, null, null);
    }

    /**
     * 정적 팩토리 메서드 - 모든 필드
     */
    public static NewsletterTemplateCollectionChart of(String collectionChartType, String targetDatabaseName,
                                                     String targetTableName, String queryConditionContent,
                                                     String tableColumnName, String color) {
        return new NewsletterTemplateCollectionChart(collectionChartType, targetDatabaseName, targetTableName,
                                                   queryConditionContent, tableColumnName, color);
    }
}
