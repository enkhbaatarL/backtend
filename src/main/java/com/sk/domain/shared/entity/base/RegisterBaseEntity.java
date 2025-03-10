package com.sk.domain.shared.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class RegisterBaseEntity {

    @CreatedBy
    @Column(name = "crt_usr_id", updatable = false, length = 50)
    @Comment("생성사용자ID")
    @ColumnDefault("SYSTEM")
    protected String createUserId;

    @CreatedDate
    @Column(name = "crt_dtm", updatable = false, nullable = false)
    @Comment("생성일시")
    @ColumnDefault("CURRENT_TIMESTAMP")
    protected LocalDateTime createDt;

    public RegisterBaseEntity(String createUserId, LocalDateTime createDt) {
        this.createUserId = createUserId;
        this.createDt = createDt;
    }

}
