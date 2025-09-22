package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 뉴스레터 수집 차트 정보 Entity
 */
@Entity
@Table(name = "tmd_nws_ltr_cltn_chrt")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("뉴스레터 수집 차트 정보")
public class NewsletterCollectionChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_cltn_chrt_id", columnDefinition = "INT UNSIGNED")
    private Long newsletterCollectionChartId;

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

    @Column(name = "cltn_chrt_type", length = 50, nullable = false)
    @Comment("수집차트 유형")
    private String collectionChartType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_pub_id", nullable = false, referencedColumnName = "nws_ltr_pub_id")
    @Comment("뉴스레터발행ID")
    private NewsletterPublishHistory newsletterPublishHistory;

    @Column(name = "start_date", nullable = false)
    @Comment("시작일시")
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    @Comment("끝일시")
    private LocalDateTime endDate;

}
