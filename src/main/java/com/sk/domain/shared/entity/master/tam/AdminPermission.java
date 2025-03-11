package com.sk.domain.shared.entity.master.tam;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.TmdBaseEntity;
import com.sk.domain.shared.entity.compositeKey.tam.AdminPermissionId;
import com.sk.domain.shared.entity.master.tmd.User;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TAM_관리자허용IP내역")
@Table(name = "tam_adminpmsnl")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminPermission extends TmdBaseEntity {
    
    @EmbeddedId
    private AdminPermissionId id;

    @MapsId("userNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", nullable = false, referencedColumnName = "usr_no")
    @Comment("사용자")
    private User user;
}
