package com.sk.domain.shared.entity.master.tcm;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "tcm_prwrdl")
@Comment("TCM_금칙어내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProhibitedWord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prwrd_sn", columnDefinition = "int unsigned")
    @Comment("금칙어일련번호")
    private Integer prohibitedWordSn;

    @Column(name = "prwrd_nm", length = 100, nullable = false, unique = true)
    @Comment("금칙어명")
    private String prohibitedWordName;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

}
