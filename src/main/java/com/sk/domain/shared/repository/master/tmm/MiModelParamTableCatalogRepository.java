package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiModelParamTableCatalog;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 수급모델 파라미터 화면 표시명 카탈로그 조회 (Agent 소유 테이블 read-only).
 */
public interface MiModelParamTableCatalogRepository extends JpaRepository<MiModelParamTableCatalog, Long> {

    /**
     * 현재 활성 카탈로그(활성 버전 중 최대 catalog_id)에서 화면 노출 항목을 파라미터 ID로 조회
     */
    @Query("""
        SELECT i
        FROM MiModelParamTableCatalog i
        WHERE i.catalogId = (
                SELECT MAX(c.catalogId)
                FROM MiModelLogicalTableCatalog c
                WHERE c.isUsed = true
                  AND c.isDeleted = false
            )
          AND i.modelParamId IN :modelParamIds
          AND i.isUiVisible = true
          AND i.isDeleted = false
        ORDER BY i.modelParamId, i.displayOrder, i.catalogItemId
        """)
    List<MiModelParamTableCatalog> findVisibleInCurrentCatalog(@Param("modelParamIds") Collection<Long> modelParamIds);

}
