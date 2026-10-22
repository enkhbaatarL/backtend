package com.sk.domain.shared.entity.master.tmm;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Immutable;

/**
 * 수급모델 파라미터 화면 표시명 카탈로그 (Agent 소유 테이블 — Backend는 read-only).
 *
 * tmm_mi_model_param_m.model_table(table1 등)은 제안/엑셀 반영 위치를 식별하는 시스템 key라 화면 라벨로 쓸 수 없어,
 * 파라미터(model_param_id)별 화면 표시명을 별도로 관리한다. 노출 조건은 ui_visible_yn=Y.
 */
@Getter
@Entity
@Immutable
@Comment("수급모델 파라미터 화면 표시명 카탈로그")
@Table(name = "tmm_mi_model_param_table_catalog_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiModelParamTableCatalog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catalog_item_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("카탈로그 항목 ID")
    private Long catalogItemId;

    @Column(name = "catalog_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("카탈로그 ID (tmm_mi_model_logical_table_catalog_m)")
    private Long catalogId;

    @Column(name = "model_param_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("수급모델 파라미터 ID (tmm_mi_model_param_m)")
    private Long modelParamId;

    @Column(name = "logical_table_code", length = 150, nullable = false)
    @Comment("UI 항목 식별 코드 (예: annual_power_mix_adjustment)")
    private String logicalTableCode;

    @Column(name = "display_name", length = 255, nullable = false)
    @Comment("화면 표시명 (예: Annual Power Mix Adjustment)")
    private String displayName;

    @Column(name = "display_order", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("표시 순서")
    private Integer displayOrder;

    @Column(name = "ui_visible_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("화면 노출 여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUiVisible;

}
