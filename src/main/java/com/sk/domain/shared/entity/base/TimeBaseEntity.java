package com.sk.domain.shared.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class TimeBaseEntity {

    @CreatedDate
    @Column(name = "crt_dtm", updatable = false, nullable = false, columnDefinition = "datetime default CURRENT_TIMESTAMP")
    @Comment("생성일시")
    protected LocalDateTime createDt;

    @LastModifiedDate
    @Column(name = "mod_dtm", nullable = false, columnDefinition = "datetime default CURRENT_TIMESTAMP")
    @Comment("수정일시")
    protected LocalDateTime modifyDt;

    public TimeBaseEntity(LocalDateTime createDt, LocalDateTime modifyDt) {
        this.createDt = createDt;
        this.modifyDt = modifyDt;
    }

}
