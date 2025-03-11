package com.sk.domain.shared.entity.compositeKey.tam;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class AdminPermissionId implements Serializable {

    @Column(name = "usr_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자번호")
    private Integer userNo; // tmd_usrm과 일치하도록 Integer 사용

    @Column(name = "ipadr", nullable = false, length = 15)
    @Comment("아이피주소")
    private String ipAddress;

    @Column(name = "aply_dtm", nullable = false)
    @Comment("적용일시")
    private LocalDateTime applyDt;
}