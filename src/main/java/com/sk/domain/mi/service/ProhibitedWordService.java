package com.sk.domain.mi.service;

import com.sk.domain.shared.entity.master.tcm.ProhibitedWord;
import com.sk.domain.shared.repository.master.tcm.ProhibitedWordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProhibitedWordService {

    private final ProhibitedWordRepository prohibitedWordRepository;
    private final AtomicReference<Set<String>> prohibitedWordsCache = new AtomicReference<>(Set.of());

    /**
     * 애플리케이션 시작 시 금칙어 캐시를 초기화합니다.
     */
    @PostConstruct
    public void initializeCache() {
        log.info("Initializing ProhibitedWord cache...");
        refreshCache();
    }

    /**
     * 1시간마다 금칙어 캐시를 갱신합니다. (fixedRateString = default 1시간(ms))
     */
    @Scheduled(fixedRateString = "${features.scheduling.prohibited-word.refresh-rate:3600000}")
    public void refreshCache() {
        log.info("Refreshing ProhibitedWord cache...");
        try {
            Set<String> newWords = prohibitedWordRepository.findByIsUsedTrueAndIsDeletedFalse()
                .stream()
                .map(ProhibitedWord::getProhibitedWordName)
                .collect(Collectors.toUnmodifiableSet());

            // 캐시 교체
            prohibitedWordsCache.set(newWords);

            log.info("ProhibitedWord cache refreshed successfully with {} words.", prohibitedWordsCache.get().size());
        } catch (Exception e) {
            log.error("Failed to refresh ProhibitedWord cache", e);
        }
    }

    /**
     * 주어진 텍스트에 금칙어가 포함되어 있는지 확인합니다.
     * 이 메소드는 DB에 접근하지 않고 메모리 캐시만을 사용합니다.
     *
     * @param text 검사할 텍스트
     * @return 금칙어 포함 여부
     */
    public boolean containsProhibitedWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // 캐시된 금칙어 목록을 순회하며 확인
        return prohibitedWordsCache.get().stream().anyMatch(text::contains);
    }

}
