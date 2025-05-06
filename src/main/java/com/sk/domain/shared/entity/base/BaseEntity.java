package com.sk.domain.shared.entity.base;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
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

    // note: [2025-05-06] 비 기본키의 ManyToOne 매핑은 정상적으로 프록시 객체를 만들어내지 못하여 N+1 문제가 발생함 (DeferredResultSetAccess)
//    // 사용자 연관관계 추가
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumnsOrFormulas({
//            @JoinColumnOrFormula(column = @JoinColumn(name = "crt_usr_logn_id", referencedColumnName = "usr_logn_id", insertable = false, updatable = false)),
//            @JoinColumnOrFormula(formula = @JoinFormula(value = "'ADMIN'", referencedColumnName = "usr_role_cd"))
//    })
//    private User creator;
//
//    // 사용자 연관관계 추가
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumnsOrFormulas({
//            @JoinColumnOrFormula(column = @JoinColumn(name = "mod_usr_logn_id", referencedColumnName = "usr_logn_id", insertable = false, updatable = false)),
//            @JoinColumnOrFormula(formula = @JoinFormula(value = "'ADMIN'", referencedColumnName = "usr_role_cd"))
//    })
//    private User modifier;

    public BaseEntity(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted) {
        super(createDt, modifyDt);
        this.createUserId = createUserId;
        this.modifyUserId = modifyUserId;
        this.isDeleted = isDeleted;
    }

    public void softDelete() {
        this.isDeleted = Boolean.TRUE;
    }

    // todo 지울 예정
    public String accessCreatorNm() {
//        return OptionalUtil.safelyAccess(() -> this.creator.getUserNm()).orElse(null);
        return null;
    }

    // todo 지울 예정
    public String accessModifierNm() {
//        return OptionalUtil.safelyAccess(() -> this.modifier.getUserNm()).orElse(null);
        return null;
    }

    // todo 지울 예정
    public String accessCreatorDeptNm() {
//        return OptionalUtil.safelyAccess(() -> this.creator.getDepartmentNm()).orElse(null);
        return null;
    }

    // todo 지울 예정
    public String accessModifierDeptNm() {
//        return OptionalUtil.safelyAccess(() -> this.modifier.getDepartmentNm()).orElse(null);
        return null;
    }

    // todo 지울 예정
    public String accessCreatorCompanyNm() {
//        return OptionalUtil.safelyAccess(() -> this.creator.getCustomerCompany().getCustomerCompanyNm()).orElse(null);
        return null;
    }

    // todo 지울 예정
    public String accessModifierCompanyNm() {
//        return OptionalUtil.safelyAccess(() -> this.modifier.getCustomerCompany().getCustomerCompanyNm()).orElse(null);
        return null;
    }

}
