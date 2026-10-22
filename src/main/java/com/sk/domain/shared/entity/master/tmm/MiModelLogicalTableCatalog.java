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
 * 수급모델 논리 테이블 카탈로그 버전 마스터 (Agent 소유 테이블 — Backend는 read-only).
 *
 * 화면 표시명 카탈로그(tmm_mi_model_param_table_catalog_m)의 버전 헤더다.
 * 활성(use_yn=Y, del_yn=N) 버전 중 catalog_id가 가장 큰 것을 현재 카탈로그로 사용한다.
 */
@Getter
@Entity
@Immutable
@Comment("수급모델 논리 테이블 카탈로그 버전 마스터")
@Table(name = "tmm_mi_model_logical_table_catalog_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiModelLogicalTableCatalog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catalog_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("카탈로그 ID")
    private Long catalogId;

    @Column(name = "catalog_version", length = 100, nullable = false)
    @Comment("카탈로그 버전 (예: 20261010-v3-final)")
    private String catalogVersion;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용 여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

}
