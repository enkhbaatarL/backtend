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
@Comment("TMD_뉴스레터템플릿섹션필드")
@Table(name = "tmd_nwsltrtmpltsctnfld")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterTemplateSectionField extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_tmplt_sctn_fld_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿섹션필드ID")
    private Long newsletterTemplateSectionFieldId;

    @Column(name = "nws_ltr_tmplt_sctn_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿섹션ID")
    private Long sectionId;

    @Column(name = "fld_key", nullable = false, length = 100)
    @Comment("필드키")
    private String fieldKey;

    @Column(name = "fld_nm", nullable = false, length = 200)
    @Comment("필드명")
    private String fieldName;

    @Column(name = "fld_ord", nullable = false, columnDefinition = "int unsigned")
    @Comment("필드순서")
    private Integer fieldOrder;

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
    public NewsletterTemplateSectionField(
        Long newsletterTemplateSectionFieldId,
        Long sectionId,
        String fieldKey,
        String fieldName,
        Integer fieldOrder,
        String isEditableYn,
        String isRequiredYn,
        String fieldMeta
    ) {
        this.newsletterTemplateSectionFieldId = newsletterTemplateSectionFieldId;
        this.sectionId = sectionId;
        this.fieldKey = fieldKey;
        this.fieldName = fieldName;
        this.fieldOrder = fieldOrder;
        this.isEditableYn = isEditableYn;
        this.isRequiredYn = isRequiredYn;
        this.fieldMeta = fieldMeta;
    }
}
