package com.sk.domain.shared.entity.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    @Column(name = "register_dt", updatable = false, nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    protected LocalDateTime registerDt;

    @LastModifiedDate
    @Column(name = "modify_dt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    protected LocalDateTime modifyDt;

    public TimeBaseEntity(LocalDateTime registerDt, LocalDateTime modifyDt) {
        this.registerDt = registerDt;
        this.modifyDt = modifyDt;
    }

}
