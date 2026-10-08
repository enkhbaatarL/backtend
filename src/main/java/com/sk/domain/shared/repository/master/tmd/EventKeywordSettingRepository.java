package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.EventKeywordSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventKeywordSettingRepository extends JpaRepository<EventKeywordSetting, Long> {

    List<EventKeywordSetting> findAllByUser_UserNoAndIsDeletedFalseOrderByKeywordSettingIdAsc(Integer userNo);

    long countByUser_UserNoAndIsDeletedFalse(Integer userNo);

    /**
     * 해지된 row를 포함해 조회한다(재등록 시 복구 대상 탐색).
     * keywd_nm 비교는 DB collation(utf8mb4_0900_ai_ci)에 따라 대소문자를 무시한다.
     */
    Optional<EventKeywordSetting> findByUser_UserNoAndKeywordName(Integer userNo, String keywordName);

    Optional<EventKeywordSetting> findByKeywordSettingIdAndUser_UserNoAndIsDeletedFalse(Long keywordSettingId, Integer userNo);
}
