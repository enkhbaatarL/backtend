package com.sk.domain.mi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.response.autocomplete.AutocompleteTermData;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutocompleteService {
    // prefix 검색용 (사전순)
    private static final String TRIE_KEY = "autocomplete:trie";
    // 빈도수 정렬용 (빈도수순)
    private static final String FREQ_SORTED_KEY = "autocomplete:freq_sorted:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mi.autocomplete.initial-load.enabled:false}")
    private boolean initialLoadEnabled;

    @PostConstruct
    public void loadFromJsonFile() {
        if (!initialLoadEnabled) {
            return;
        }

        try {
            Resource resource = new ClassPathResource("static/json/autocomplete/financial-terms.json");

            List<AutocompleteTermData> terms = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<AutocompleteTermData>>() {}
            );

            Map<String, Integer> termMap = terms.stream()
                .collect(Collectors.toMap(
                    AutocompleteTermData::getTerm,
                    AutocompleteTermData::getFrequency
                ));

            bulkAddTerms(termMap);

            log.info("Successfully loaded {} financial terms", terms.size());
        } catch (IOException e) {
            log.error("Failed to load financial terms", e);
            throw new RuntimeException("Failed to initialize autocomplete data", e);
        }
    }

    public void addTerm(String term, int frequency) {
        String termLower = term.toLowerCase();

        // 1. 사전순 저장
        redisTemplate.opsForZSet().add(TRIE_KEY, termLower, 0);

        // 2. 각 prefix별로 빈도수 정렬된 Sorted Set 생성
        for (int i = 1; i <= Math.min(termLower.length(), 10); i++) {
            String prefix = termLower.substring(0, i);
            redisTemplate.opsForZSet().add(
                FREQ_SORTED_KEY + prefix,
                termLower,
                frequency
            );
        }
    }

    public void bulkAddTerms(Map<String, Integer> terms) {
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                terms.forEach((term, freq) -> {
                    String termLower = term.toLowerCase();

                    // 사전순 저장
                    operations.opsForZSet().add(TRIE_KEY, termLower, 0);

                    // prefix별 빈도수 저장 (최대 10글자까지)
                    for (int i = 1; i <= Math.min(termLower.length(), 10); i++) {
                        String prefix = termLower.substring(0, i);
                        operations.opsForZSet().add(
                            FREQ_SORTED_KEY + prefix,
                            termLower,
                            freq
                        );
                    }
                });
                return null;
            }
        });
    }

    // 빠른 자동완성 검색
    public List<AutocompleteTermData> autocomplete(String prefix, int limit) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String prefixLower = prefix.toLowerCase();
        String freqKey = FREQ_SORTED_KEY + prefixLower;

        // prefix에 해당하는 Sorted Set이 있으면 바로 사용
        if (Boolean.TRUE.equals(redisTemplate.hasKey(freqKey))) {
            // Redis에서 이미 빈도수 순으로 정렬된 상태로 가져옴
            Set<ZSetOperations.TypedTuple<String>> results =
                redisTemplate.opsForZSet().reverseRangeWithScores(freqKey, 0, limit - 1);

            if (results != null) {
                return results.stream()
                    .filter(tuple -> tuple.getValue() != null && tuple.getScore() != null)
                    .map(tuple -> new AutocompleteTermData(
                        tuple.getValue(),
                        tuple.getScore().intValue()
                    ))
                    .collect(Collectors.toList());
            }
        }

        // prefix Sorted Set이 없으면 fallback (긴 prefix의 경우)
        return fallbackAutocomplete(prefixLower, limit);
    }

    // fallback: 기존 방식 (긴 prefix용)
    private List<AutocompleteTermData> fallbackAutocomplete(String prefix, int limit) {
        Set<String> matches = redisTemplate.opsForZSet()
            .rangeByLex(TRIE_KEY,
                Range.closed(prefix, prefix + "\uffff"),
                Limit.limit().count(limit * 2));

        if (matches == null || matches.isEmpty()) {
            return Collections.emptyList();
        }

        return matches.stream()
            .limit(limit)
            .map(term -> new AutocompleteTermData(term, 0))
            .collect(Collectors.toList());
    }

    public void incrementFrequency(String term) {
        String termLower = term.toLowerCase();

        for (int i = 1; i <= Math.min(termLower.length(), 10); i++) {
            String prefix = termLower.substring(0, i);
            redisTemplate.opsForZSet().incrementScore(
                FREQ_SORTED_KEY + prefix,
                termLower,
                1
            );
        }
    }

    public void clearAll() {
        redisTemplate.delete(TRIE_KEY);
        Set<String> keys = redisTemplate.keys(FREQ_SORTED_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("Cleared all autocomplete data from Redis");
    }
}
