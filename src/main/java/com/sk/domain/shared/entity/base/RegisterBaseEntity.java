package com.sk.domain.shared.entity.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    @Column(name = "register_id", updatable = false, length = 50)
    protected String registerId;

    @CreatedDate
    @Column(name = "register_dt", updatable = false, nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    protected LocalDateTime registerDt;

    public RegisterBaseEntity(String registerId, LocalDateTime registerDt) {
        this.registerId = registerId;
        this.registerDt = registerDt;
    }

}
