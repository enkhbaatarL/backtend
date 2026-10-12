package com.sk.domain.shared.repository.master.tmd;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.repository.master.tlm.projection.NotificationKeyRow;

/**
 * My Notifications 통합 키 조회
 * - 상품 임계치 설정(tmd_gdscrtvlsetl, 데이터소스 단위)과 이벤트 키워드 알림(tmd_mi_evnt_keywdnotil)을 UNION ALL로 합쳐 DB에서 정렬·페이징한다
 * - 상품 분기 조건은 기존 알림 설정한 정형 데이터 목록과 동일 (미삭제 상품·데이터·데이터소스·설정 + 본인 + 상품데이터명 검색)
 * - 이벤트 키워드 분기는 includeEventKeyword(SKGAS 여부)가 false면 0건, 검색은 이벤트명 스냅샷
 * - 정렬: 상품 = 데이터소스 내 최근 설정 등록일시, 키워드 = 알림 발생일시 내림차순. 동일 시각은 유형·ID로 순서 고정
 * - 검색 패턴은 '!' 이스케이프된 소문자 LIKE 패턴 (hasSearch가 false면 검색 조건 없음)
 */
public interface MyNotificationRepository extends Repository<GoodsCriticalValueSetting, Integer> {

    @Query(value = """
        SELECT t.noti_type AS notificationType, t.noti_id AS notificationId
          FROM (
                SELECT 'GOODS_THRESHOLD' AS noti_type, d.gds_data_src_no AS noti_id, MAX(s.crt_dtm) AS noti_dtm
                  FROM tmd_gdscrtvlsetl s
                  JOIN tmd_gdsdatasrcd d ON d.gds_data_src_no = s.gds_data_src_no
                  JOIN tmd_gdsdatam g ON g.gds_data_no = d.gds_data_no
                  JOIN tmd_gdsm gs ON gs.gds_no = g.gds_no
                 WHERE s.del_yn = 'N'
                   AND s.usr_no = :userNo
                   AND d.del_yn = 'N'
                   AND g.del_yn = 'N'
                   AND gs.del_yn = 'N'
                   AND (:hasSearch = FALSE OR LOWER(g.gds_data_nm) LIKE :searchPattern ESCAPE '!')
                 GROUP BY d.gds_data_src_no
                UNION ALL
                SELECT 'EVENT_KEYWORD', n.keywd_noti_id, n.crt_dtm
                  FROM tmd_mi_evnt_keywdnotil n
                 WHERE :includeEventKeyword = TRUE
                   AND n.del_yn = 'N'
                   AND n.usr_no = :userNo
                   AND (:hasSearch = FALSE OR LOWER(n.evnt_snpsht_nm) LIKE :searchPattern ESCAPE '!')
               ) t
         ORDER BY t.noti_dtm DESC, t.noti_type ASC, t.noti_id DESC
        """,
        countQuery = """
        SELECT (SELECT COUNT(DISTINCT d.gds_data_src_no)
                  FROM tmd_gdscrtvlsetl s
                  JOIN tmd_gdsdatasrcd d ON d.gds_data_src_no = s.gds_data_src_no
                  JOIN tmd_gdsdatam g ON g.gds_data_no = d.gds_data_no
                  JOIN tmd_gdsm gs ON gs.gds_no = g.gds_no
                 WHERE s.del_yn = 'N'
                   AND s.usr_no = :userNo
                   AND d.del_yn = 'N'
                   AND g.del_yn = 'N'
                   AND gs.del_yn = 'N'
                   AND (:hasSearch = FALSE OR LOWER(g.gds_data_nm) LIKE :searchPattern ESCAPE '!'))
             + (SELECT COUNT(*)
                  FROM tmd_mi_evnt_keywdnotil n
                 WHERE :includeEventKeyword = TRUE
                   AND n.del_yn = 'N'
                   AND n.usr_no = :userNo
                   AND (:hasSearch = FALSE OR LOWER(n.evnt_snpsht_nm) LIKE :searchPattern ESCAPE '!'))
        """,
        nativeQuery = true)
    Page<NotificationKeyRow> findMyNotificationKeys(
        @Param("userNo") Integer userNo,
        @Param("includeEventKeyword") boolean includeEventKeyword,
        @Param("hasSearch") boolean hasSearch,
        @Param("searchPattern") String searchPattern,
        Pageable pageable
    );

}
