package com.sk.domain.shared.entity.compositeKey.tcm;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class BulletinBoardUserMappingId {

    @Column(name = "bbm_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("게시글번호")
    private Integer bbmNo;

    @Column(name = "usr_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자번호")
    private Integer userNo;

    @Builder
    public BulletinBoardUserMappingId(Integer bbmNo, Integer userNo) {
        this.bbmNo = bbmNo;
        this.userNo = userNo;
    }

    public static BulletinBoardUserMappingId of(Integer bbmNo, Integer userNo) {
        return BulletinBoardUserMappingId.builder()
                .bbmNo(bbmNo)
                .userNo(userNo)
                .build();
    }

}
