package com.sk.domain.mi.service.nextgen.keywordalert.scan;

import com.sk.domain.shared.entity.master.tmd.EventKeywordSetting;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.repository.master.tmm.projection.KeywordAlertEventRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 이벤트-키워드 매칭
 * - 기준: one_line_summary에 키워드 구문 포함 여부 (ngram FULLTEXT, AGAINST('"키워드"'))
 * - 미사용: 목록 검색(FullTextSearchUtil)의 토큰 OR — 여러 단어 키워드에서 오탐
 * - 쿼리: 서로 다른 키워드(대소문자 무시)마다 1회
 */
@Component
@RequiredArgsConstructor
public class EventKeywordMatcher {

    private final MiEventRepository miEventRepository;

    /**
     * @return 이벤트 ID → 매칭된 키워드 설정 목록 (매칭 없는 이벤트는 키 없음)
     */
    public Map<Long, List<EventKeywordSetting>> match(List<EventKeywordSetting> settings, List<KeywordAlertEventRow> events) {
        Map<Long, List<EventKeywordSetting>> matches = new LinkedHashMap<>();
        if (settings.isEmpty() || events.isEmpty()) {
            return matches;
        }

        List<Long> eventIds = events.stream().map(KeywordAlertEventRow::getMiEventId).toList();

        Map<String, List<EventKeywordSetting>> settingsByKeyword = new LinkedHashMap<>();
        for (EventKeywordSetting setting : settings) {
            settingsByKeyword
                    .computeIfAbsent(setting.getKeywordName().toLowerCase(Locale.ROOT), key -> new ArrayList<>())
                    .add(setting);
        }

        settingsByKeyword.forEach((keyword, keywordSettings) -> {
            for (Long eventId : miEventRepository.findEventIdsMatchingPhrase(eventIds, toPhrase(keyword))) {
                matches.computeIfAbsent(eventId, key -> new ArrayList<>()).addAll(keywordSettings);
            }
        });
        return matches;
    }

    /**
     * BOOLEAN MODE 구문 검색어. 큰따옴표 안에서는 + - * ( ) 등 연산자가 문자로 취급되므로,
     * 구문을 깨는 키워드 안의 큰따옴표만 공백으로 바꿈.
     */
    static String toPhrase(String keyword) {
        return "\"%s\"".formatted(keyword.replace('"', ' ').strip());
    }

}
