package com.sk.domain.shared.entity.base;

import java.time.LocalDateTime;
import java.util.Optional;

import com.sk.common.utils.OptionalUtil;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sk.common.converter.BooleanToYNAttributeConverter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends TimeBaseEntity {

    @CreatedBy
    @Column(name = "crt_usr_logn_id", updatable = false, length = 50, nullable = false)
    @Comment("생성사용자ID")
    @ColumnDefault("'SYSTEM'")
    protected String createUserId;

    @LastModifiedBy
    @Column(name = "mod_usr_logn_id", length = 50, nullable = false)
    @Comment("수정사용자ID")
    @ColumnDefault("'SYSTEM'")
    protected String modifyUserId;

    @Column(name = "del_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("삭제여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isDeleted;

    // 사용자 연관관계 추가
    @ManyToOne
    @JoinColumn(name = "crt_usr_logn_id", referencedColumnName = "usr_logn_id", insertable = false, updatable = false)
    private User creator;

    // 사용자 연관관계 추가
    @ManyToOne
    @JoinColumn(name = "mod_usr_logn_id", referencedColumnName = "usr_logn_id", insertable = false, updatable = false)
    private User modifier;

    public BaseEntity(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted) {
        super(createDt, modifyDt);
        this.createUserId = createUserId;
        this.modifyUserId = modifyUserId;
        this.isDeleted = isDeleted;
    }

    public void softDelete() {
        this.isDeleted = Boolean.TRUE;
    }

    public String accessCreatorNm() {
        return OptionalUtil.safelyAccess(() -> this.creator.getCreator().getUserNm()).orElse(null);
    }

    public String accessModifierNm() {
        return OptionalUtil.safelyAccess(() -> this.modifier.getModifier().getUserNm()).orElse(null);
    }

    public String accessCreatorDeptNm() {
        return OptionalUtil.safelyAccess(() -> this.creator.getCreator().getDepartmentNm()).orElse(null);
    }

    public String accessModifierDeptNm() {
        return OptionalUtil.safelyAccess(() -> this.modifier.getModifier().getDepartmentNm()).orElse(null);
    }

    public String accessCreatorCompanyNm() {
        return OptionalUtil.safelyAccess(() -> this.creator.getCreator().getCustomerCompany().getCustomerCompanyNm()).orElse(null);
    }

    public String accessModifierCompanyNm() {
        return OptionalUtil.safelyAccess(() -> this.modifier.getModifier().getCustomerCompany().getCustomerCompanyNm()).orElse(null);
    }

}
