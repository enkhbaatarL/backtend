package com.sk.domain.shared.entity.master.tmm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMM_Impact")
@Table(name = "tmm_impactm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverrides({
        @AttributeOverride(name = "createUserId", column = @Column(name = "crt_user_id", length = 50, nullable = false)),
        @AttributeOverride(name = "modifyUserId", column = @Column(name = "mod_user_id", length = 50, nullable = false))
})
public class Impact extends BaseEntity {

    @Id
    @Column(name = "impact_id", length = 64, nullable = false)
    @Comment("Impact ID")
    private String impactId;

    @Column(name = "mi_event_id", length = 32, nullable = false)
    @Comment("MI Event ID FK")
    private String miEventId;

    @Column(name = "updated_at")
    @Comment("업데이트 일시")
    private LocalDateTime updatedAt;

    @Builder
    public Impact(String impactId, String miEventId, LocalDateTime updatedAt) {
        this.impactId = impactId;
        this.miEventId = miEventId;
        this.updatedAt = updatedAt;
    }
}
