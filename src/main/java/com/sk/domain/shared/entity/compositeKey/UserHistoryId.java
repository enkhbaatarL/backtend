package com.sk.domain.shared.entity.compositeKey;

import java.io.Serializable;
import java.time.LocalDateTime;

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
public class UserHistoryId implements Serializable {

    @Column(name = "usr_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자번호")
    private Integer userNo;

    @Column(name = "aply_dtm", nullable = false, columnDefinition = "datetime")
    @Comment("적용일시")
    private LocalDateTime applyDt;

    @Builder
    public UserHistoryId(Integer userNo, LocalDateTime applyDt) {
        this.userNo = userNo;
        this.applyDt = applyDt;
    }
}