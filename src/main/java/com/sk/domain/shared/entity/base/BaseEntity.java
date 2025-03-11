package com.sk.domain.shared.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends TimeBaseEntity {

    @CreatedBy
    @Column(name = "crt_usr_logn_id", updatable = false, length = 50, nullable = false)
    @Comment("생성사용자ID")
    @ColumnDefault("SYSTEM")
    protected String createUserId;

    @LastModifiedBy
    @Column(name = "mod_usr_logn_id", length = 50, nullable = false)
    @Comment("수정사용자ID")
    @ColumnDefault("SYSTEM")
    protected String modifyUserId;

    public BaseEntity(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId) {
        super(createDt, modifyDt);
        this.createUserId = createUserId;
        this.modifyUserId = modifyUserId;
    }

}
