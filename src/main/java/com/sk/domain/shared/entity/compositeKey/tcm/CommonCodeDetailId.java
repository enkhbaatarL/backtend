package com.sk.domain.shared.entity.compositeKey.tcm;

import java.io.Serializable;

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
public class CommonCodeDetailId implements Serializable {

    @Column(name = "cd_grp_id", nullable = false, length = 50)
    @Comment("코드그룹ID")
    private String codeGroupId;

    @Column(name = "cd_id", nullable = false, length = 30)
    @Comment("코드ID")
    private String codeId;

    @Builder
    public CommonCodeDetailId(String codeGroupId, String codeId) {
        this.codeGroupId = codeGroupId;
        this.codeId = codeId;
    }
}