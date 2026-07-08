package com.sk.domain.shared.entity.master.tmm;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.converter.BasecaseReadModeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.nextgen.BasecaseReadMode;
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
 * 수급모델 파라미터 마스터 (Agent 소유 테이블 — Backend는 read-only).
 *
 * Basecase Excel에서 파라미터별로 어느 파일/시트/테이블의 어느 셀을 읽고 쓸지를 정의하는
 * Agent configuration이다. seed 적재/변경의 주체는 Agent이며, Backend는 확정값 Excel 병합 시
 * 셀 좌표 해석 근거(column roles, header/data row, read mode)로만 조회한다.
 *
 * 병합 좌표는 payload의 필드명이 아니라 이 마스터의 basecaseColumnRoles
 * (예: B:Year,C:Country,F:% change (+/-)) 컬럼 문자를 기준으로 해석해야 한다
 * — 필드명과 Excel 헤더 라벨이 다른 파라미터가 존재한다(예: GDP).
 */
@Getter
@Entity
@Immutable
@Comment("수급모델 파라미터 마스터")
@Table(name = "tmm_mi_model_param_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiModelParam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_param_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("수급모델 파라미터 ID")
    private Long modelParamId;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용 여부 (제안 대상 파라미터 여부)")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Column(name = "parameter_name", length = 150, nullable = false)
    @Comment("수급모델 파라미터명 (payload parameterName과 매칭되는 자연키)")
    private String parameterName;

    @Column(name = "parameter_hint", length = 500, nullable = false)
    @Comment("수급모델 파라미터 설명")
    private String parameterHint;

    @Column(name = "model_file", length = 100, nullable = false)
    @Comment("모델 파일 구분 (Demand/Supply/Trade — Agent title-case 표기)")
    private String modelFile;

    @Column(name = "model_sheet", length = 150, nullable = false)
    @Comment("모델 시트명")
    private String modelSheet;

    @Column(name = "model_table", length = 50, nullable = false)
    @Comment("모델 시트 내 테이블명 (한 시트에 표가 복수일 때의 논리 구분, 좌표는 column roles가 결정)")
    private String modelTable;

    @Column(name = "semantic_axes", length = 500)
    @Comment("의미 축 설명")
    private String semanticAxes;

    @Convert(converter = BasecaseReadModeConverter.class)
    @Column(name = "basecase_read_mode", nullable = false, length = 50)
    @Comment("Basecase 읽기 방식 (fixed_columns/matrix_time_columns)")
    private BasecaseReadMode basecaseReadMode;

    @Column(name = "basecase_header_row", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("Basecase header row (1-based)")
    private Integer basecaseHeaderRow;

    @Column(name = "basecase_data_start_row", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("Basecase data start row (1-based)")
    private Integer basecaseDataStartRow;

    @Column(name = "basecase_column_roles", length = 1000, nullable = false)
    @Comment("Basecase column role mapping (예: B:Year,C:Country,F:% change (+/-))")
    private String basecaseColumnRoles;

    @Column(name = "basecase_row_filter", length = 200)
    @Comment("Basecase row filter (예: E=CDD — 조건 행만 읽을 때)")
    private String basecaseRowFilter;

    @Column(name = "basecase_year_row", columnDefinition = "smallint unsigned")
    @Comment("Basecase year row (matrix 전용, 1-based)")
    private Integer basecaseYearRow;

    @Column(name = "basecase_month_row", columnDefinition = "smallint unsigned")
    @Comment("Basecase month row (matrix 전용, 1-based)")
    private Integer basecaseMonthRow;

    @Column(name = "basecase_period_row", columnDefinition = "smallint unsigned")
    @Comment("Basecase period row (matrix 전용, 1-based)")
    private Integer basecasePeriodRow;

    @Column(name = "basecase_value_start_col", length = 10)
    @Comment("Basecase value start column (matrix 전용, 예: G)")
    private String basecaseValueStartCol;

    @Column(name = "basecase_value_end_col", length = 10)
    @Comment("Basecase value end column (matrix 전용, 예: DA)")
    private String basecaseValueEndCol;

    @Column(name = "basecase_stop_rule", length = 100, nullable = false)
    @Comment("Basecase scan stop rule (예: stop_when_all_key_columns_empty)")
    private String basecaseStopRule;

}
