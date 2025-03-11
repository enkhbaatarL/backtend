package com.sk.domain.shared.entity.base;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sk.common.converter.BooleanToYNAttributeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TmdBaseEntity extends TimeBaseEntity {

    @CreatedBy
    @Column(name = "crt_usr_logn_id", nullable = false, updatable = false, length = 50)
    @Comment("생성사용자로그인ID")
    @ColumnDefault("'SYSTEM'")
    private String createUserLoginId;

    @LastModifiedBy
    @Column(name = "mod_usr_logn_id", nullable = false, length = 50)
    @Comment("수정사용자로그인ID")
    @ColumnDefault("'SYSTEM'")
    private String modifyUserLoginId;

    @Column(name = "del_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("삭제여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isDeleted;

}
