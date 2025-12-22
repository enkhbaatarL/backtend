package com.sk.domain.shared.entity.master.tif;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tif.RefinitivNewsDataId;
import com.sk.domain.shared.enums.news.NewsSupplier;
import com.sk.domain.shared.utils.RefinitivMetaParser;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Immutable;

/**
 * Refinitiv News Data 메타 정보 Entity (읽기 전용)
 *
 * <p>이 엔티티는 조회 전용이며 수정되지 않습니다.
 * JSON 메타 정보에서 필요한 값은 {@link RefinitivMetaParser}를 통해 추출합니다.</p>
 *
 * @see RefinitivMetaParser
 */
@Entity
@Getter
@Immutable
@Comment("TIF_Refinitiv News Data Meta_수신")
@Table(
    name = "tif_refinewsdatar_meta",
    indexes = {
        @Index(name = "tif_refinewsdatar_meta_ix01", columnList = "first_created, crt_dtm")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefinitivNewsDataMeta extends BaseEntity {

    @EmbeddedId
    private RefinitivNewsDataId id;

    @Column(name = "meta", columnDefinition = "JSON", nullable = false)
    @Comment("META 정보 (원본 JSON)")
    private String meta;

    @Builder
    public RefinitivNewsDataMeta(LocalDateTime createDt, LocalDateTime modifyDt,
                                String createUserId, String modifyUserId, Boolean isDeleted,
                                RefinitivNewsDataId id, String meta) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.meta = meta;
    }

    /**
     * JSON 메타에서 source code를 추출합니다.
     *
     * @return source code (예: "RTRS"), 없으면 null
     */
    public String getSourceCode() {
        return RefinitivMetaParser.extractSourceCode(this.meta);
    }

    /**
     * JSON 메타에서 전체 qcode를 추출합니다.
     *
     * @return full qcode (예: "NS:RTRS"), 없으면 null
     */
    public String getFullQCode() {
        return RefinitivMetaParser.extractFullQCode(this.meta);
    }

    /**
     * Source code에 매핑되는 NewsSupplier를 반환합니다.
     *
     * @return NewsSupplier enum, 매핑되지 않으면 null
     */
    public NewsSupplier getNewsSupplier() {
        String sourceCode = getSourceCode();
        return sourceCode != null ? NewsSupplier.fromCode(sourceCode) : null;
    }

    /**
     * Supplier 이름을 반환합니다.
     *
     * @return supplier 이름 (예: "Tankan News"), 매핑되지 않으면 null
     */
    public String getSupplierName() {
        NewsSupplier supplier = getNewsSupplier();
        return supplier != null ? supplier.getSupplierName() : null;
    }
}
