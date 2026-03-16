package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_뉴스레터템플릿블록필드")
@Table(name = "tmd_nwsltrtmpltblkfld")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterTemplateBlockField extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_tmplt_blk_fld_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿블록필드ID")
    private Long newsletterTemplateBlockFieldId;

    @Column(name = "nws_ltr_tmplt_blk_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿블록ID")
    private Long blockId;

    @Column(name = "fld_key", nullable = false, length = 100)
    @Comment("필드키")
    private String fieldKey;

    @Column(name = "fld_nm", nullable = false, length = 200)
    @Comment("필드명")
    private String fieldName;

    @Column(name = "fld_ord", nullable = false, columnDefinition = "int unsigned")
    @Comment("필드순서")
    private Integer fieldOrder;

    @Column(name = "col_mapping", length = 500)
    @Comment("컬럼매핑")
    private String colMapping;

    @Column(name = "is_editable_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용자수정가능여부")
    private String isEditableYn;

    @Column(name = "is_required_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("필수여부")
    private String isRequiredYn;

    @Column(name = "fld_meta", columnDefinition = "json")
    @Comment("필드메타정보")
    private String fieldMeta;

    @Builder
    public NewsletterTemplateBlockField(
        Long newsletterTemplateBlockFieldId,
        Long blockId,
        String fieldKey,
        String fieldName,
        Integer fieldOrder,
        String colMapping,
        String isEditableYn,
        String isRequiredYn,
        String fieldMeta
    ) {
        this.newsletterTemplateBlockFieldId = newsletterTemplateBlockFieldId;
        this.blockId = blockId;
        this.fieldKey = fieldKey;
        this.fieldName = fieldName;
        this.fieldOrder = fieldOrder;
        this.colMapping = colMapping;
        this.isEditableYn = isEditableYn;
        this.isRequiredYn = isRequiredYn;
        this.fieldMeta = fieldMeta;
    }
}
