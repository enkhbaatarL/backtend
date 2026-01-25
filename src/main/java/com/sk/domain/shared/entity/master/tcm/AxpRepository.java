package com.sk.domain.shared.entity.master.tcm;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "tcm_axp_rpstry")
@Comment("TCM_AXP 레포지토리 정보")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AxpRepository extends BaseEntity {

    @Id
    @Column(name = "vctr_rpstry_id", nullable = false, length = 50)
    @Comment("AXP 레포지토리 ID")
    private String vectorRepositoryId;

    @Column(name = "data_src_id", nullable = false, length = 50)
    @Comment("AXP 데이터소스 ID")
    private String dataSourceId;

    @Column(name = "cmpnt_type_id", nullable = false, length = 100)
    @Comment("컴포넌트유형ID")
    private String componentTypeId;

    @Builder
    public AxpRepository(String vectorRepositoryId, String dataSourceId, String componentTypeId) {
        this.vectorRepositoryId = vectorRepositoryId;
        this.dataSourceId = dataSourceId;
        this.componentTypeId = componentTypeId;
    }

    public void update(String dataSourceId, String componentTypeId) {
        this.dataSourceId = dataSourceId;
        this.componentTypeId = componentTypeId;
    }
}
