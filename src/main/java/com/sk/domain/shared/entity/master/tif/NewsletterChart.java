package com.sk.domain.shared.entity.master.tif;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("뉴스레터차트")
@Table(name = "tif_nws_ltr_chrt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterChart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_chrt_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터차트ID")
    private Long newsletterChartId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_ltr_pub_id", nullable = false, referencedColumnName = "nws_ltr_pub_id")
    @Comment("뉴스레터발행ID")
    private NewsletterPublishHistory newsletterPublishHistory;

    @Column(name = "code", nullable = false, length = 30)
    @Comment("차트코드")
    private String code;

    @Column(name = "timestamp", nullable = false)
    @Comment("Timestamp ( UTC ) 타임존 추가")
    private LocalDateTime timestamp;

    @Column(name = "high_1", precision = 15, scale = 4)
    @Comment("최고가")
    private BigDecimal high1;

    @Column(name = "low_1", precision = 15, scale = 4)
    @Comment("최저가")
    private BigDecimal low1;

    @Column(name = "open_prc", precision = 15, scale = 4)
    @Comment("OPEN_PRICE")
    private BigDecimal openPrice;

    @Column(name = "trdprc_1", precision = 15, scale = 4)
    @Comment("CLOSE_PRICE")
    private BigDecimal tradePrice1;

    @Column(name = "num_moves", precision = 15, scale = 4)
    @Comment("거래량 (UINT)")
    private BigDecimal numMoves;

    @Builder
    public NewsletterChart(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
        String modifyUserId,
        Boolean isDeleted, Long newsletterChartId,
        NewsletterPublishHistory newsletterPublishHistory,
        String code, LocalDateTime timestamp, BigDecimal high1, BigDecimal low1,
        BigDecimal openPrice, BigDecimal tradePrice1, BigDecimal numMoves) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.newsletterChartId = newsletterChartId;
        this.newsletterPublishHistory = newsletterPublishHistory;
        this.code = code;
        this.timestamp = timestamp;
        this.high1 = high1;
        this.low1 = low1;
        this.openPrice = openPrice;
        this.tradePrice1 = tradePrice1;
        this.numMoves = numMoves;
    }

    public static NewsletterChart createNewsletterChart(
        NewsletterPublishHistory newsletterPublishHistory,
        String code, LocalDateTime timestamp, BigDecimal high1, BigDecimal low1,
        BigDecimal openPrice, BigDecimal tradePrice1, BigDecimal numMoves) {
        return NewsletterChart.builder()
            .newsletterPublishHistory(newsletterPublishHistory)
            .code(code)
            .timestamp(timestamp)
            .high1(high1)
            .low1(low1)
            .openPrice(openPrice)
            .tradePrice1(tradePrice1)
            .numMoves(numMoves)
            .build();
    }
}
