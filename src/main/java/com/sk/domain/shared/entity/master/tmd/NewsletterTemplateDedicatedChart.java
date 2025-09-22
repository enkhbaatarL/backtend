package com.sk.domain.shared.entity.master.tmd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 뉴스레터 템플릿 전용 차트 엔티티
 */
@Entity
@Table(name = "tmd_nws_ltr_tmplt_dedi_chrt",
       indexes = {
           @Index(name = "tmd_nws_ltr_tmplt_dedi_chrt_ix01", columnList = "dedi_chrt_type")
       })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("뉴스레터 템플릿 전용 차트")
public class NewsletterTemplateDedicatedChart {

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

    @Column(name = "del_yn", nullable = false, columnDefinition = "char(1)")
    @Comment("삭제여부")
    private String deleteYn = "N";

    @Column(name = "dedi_chrt_type", nullable = false, length = 50)
    @Comment("전용차트 유형")
    private String dedicatedChartType;

    @Column(name = "code", nullable = false, length = 30)
    @Comment("코드")
    private String code;

    @Column(name = "color", length = 20)
    @Comment("색상")
    private String color;

    /**
     * 생성자
     */
    public NewsletterTemplateDedicatedChart(String dedicatedChartType, String code, String color) {
        this.dedicatedChartType = dedicatedChartType;
        this.code = code;
        this.color = color;
    }

    /**
     * 정적 팩토리 메서드
     */
    public static NewsletterTemplateDedicatedChart of(String dedicatedChartType, String code, String color) {
        return new NewsletterTemplateDedicatedChart(dedicatedChartType, code, color);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createDateTime = now;
        this.modifyDateTime = now;
        if (this.deleteYn == null) {
            this.deleteYn = "N";
        }
        if (this.createUserLoginId == null) {
            this.createUserLoginId = "SYSTEM";
        }
        if (this.modifyUserLoginId == null) {
            this.modifyUserLoginId = "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifyDateTime = LocalDateTime.now();
    }

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return "Y".equals(this.deleteYn);
    }

    /**
     * 논리적 삭제 처리
     */
    public void delete() {
        this.deleteYn = "Y";
        this.modifyDateTime = LocalDateTime.now();
    }
}
