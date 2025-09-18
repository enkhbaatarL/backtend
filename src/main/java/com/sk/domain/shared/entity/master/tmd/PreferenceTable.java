package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("참고테이블데이터")
@Table(name = "tmd_pref_tbl",
       uniqueConstraints = @UniqueConstraint(name = "tmd_pref_tbl_ux01",
                                           columnNames = {"nws_ltr_pub_id", "pref_tbl_cd"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreferenceTable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pref_tbl_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("참고테이블데이터ID")
    private Integer preferenceTableId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_pub_id", nullable = false, referencedColumnName = "nws_ltr_pub_id")
    @Comment("뉴스레터발행ID")
    private NewsletterPublishHistory newsletterPublishHistory;

    @Column(name = "pref_tbl_cd", nullable = false, length = 20)
    @Comment("참고테이블코드 OIL_PRC_TRND")
    private String preferenceTableCode;

    @Lob
    @Column(name = "json_value", nullable = false, columnDefinition = "TEXT")
    @Comment("JSON_VALUE")
    private String jsonValue;

    @Builder
    public PreferenceTable(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer preferenceTableId, NewsletterPublishHistory newsletterPublishHistory,
            String preferenceTableCode, String jsonValue) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.preferenceTableId = preferenceTableId;
        this.newsletterPublishHistory = newsletterPublishHistory;
        this.preferenceTableCode = preferenceTableCode;
        this.jsonValue = jsonValue;
    }

    public static PreferenceTable createPreferenceTable(NewsletterPublishHistory newsletterPublishHistory,
            String preferenceTableCode, String jsonValue) {
        return PreferenceTable.builder()
                .newsletterPublishHistory(newsletterPublishHistory)
                .preferenceTableCode(preferenceTableCode)
                .jsonValue(jsonValue)
                .build();
    }
}
