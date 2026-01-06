package com.sk.domain.mi.service;


import com.sk.domain.mi.dto.response.news.NewsTopicListRes;
import com.sk.domain.mi.dto.response.news.TopicInfoRes;
import com.sk.domain.shared.entity.master.tmd.NewsTopic;
import com.sk.domain.shared.repository.master.tmd.NewsTopicRelationshipRepository;
import com.sk.domain.shared.repository.master.tmd.NewsTopicRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiTopicTrackerService {

    private final NewsTopicRepository newsTopicRepository;
    private final NewsTopicRelationshipRepository newsTopicRelationshipRepository;

    /**
     * 활성화된 토픽 목록 조회
     * - del_yn = 'N', use_yn = 'Y' 조건
     * - 토픽 ID 기준 오름차순 정렬
     *
     * @return 토픽 정보 목록
     */
    public List<TopicInfoRes> getTopicList(int userNo) {
        log.debug("활성화된 토픽 목록 조회 시작");

        List<NewsTopic> activeTopics = newsTopicRepository.findByIsDeletedFalseAndUseYnOrderByNewsTopicIdAsc("Y");

        // TODO: 사용자별로 등록 후 승인된 토픽 추가해야함

        List<TopicInfoRes> result = activeTopics.stream()
            .map(this::toTopicInfoRes)
            .toList();

        log.debug("활성화된 토픽 {} 건 조회 완료", result.size());

        return result;
    }

    /**
     * 뉴스 토픽별 뉴스 목록 조회
     * - 기준일자와 토픽 ID로 필터링
     * - newsTopicId가 null이면 전체 토픽의 뉴스 조회
     * - firstCreated 최신순 정렬
     *
     * @param crtrDt 기준일자 (YYYYMMDD)
     * @param newsTopicId 토픽 ID (null이면 전체 조회)
     * @return 뉴스 토픽 목록
     */
    public List<NewsTopicListRes> getNewsTopicList(String crtrDt, Long newsTopicId) {
        log.debug("뉴스 토픽 목록 조회 시작 - crtrDt: {}, newsTopicId: {}", crtrDt, newsTopicId);

        boolean filterByTopic = newsTopicId != null;

        List<Object[]> results = newsTopicRelationshipRepository.findNewsTopicRelationshipsWithRegions(
            crtrDt,
            newsTopicId,
            filterByTopic
        );

        List<NewsTopicListRes> response = results.stream()
            .map(this::convertToNewsTopicListRes)
            .toList();

        log.debug("뉴스 토픽 {} 건 조회 완료", response.size());

        return response;
    }

    /**
     * NewsTopic 엔티티를 TopicInfoRes DTO로 변환
     *
     * @param newsTopic 뉴스 토픽 엔티티
     * @return TopicInfoRes DTO
     */
    private TopicInfoRes toTopicInfoRes(NewsTopic newsTopic) {
        // TODO: 아이콘 이미지 경로 조회 로직 추가 필요
        return TopicInfoRes.of(
            newsTopic.getNewsTopicId(),
            null,  // 아이콘 이미지 경로는 추후 구현
            newsTopic.getNewsTopicName(),
            newsTopic.getUserRegisteredYn()
        );
    }

    /**
     * Object[] 쿼리 결과를 NewsTopicListRes DTO로 변환
     *
     * @param row [newsTopicRelationshipId, crtrDt, newsTopicId, newsTopicName,
     *             newsSummaryId, dataRegistrationSn, firstCreated, briefSummary,
     *             summary, title, providerId, regions]
     * @return NewsTopicListRes
     */
    private NewsTopicListRes convertToNewsTopicListRes(Object[] row) {
        int idx = 0;

        Long newsTopicRelationshipId = row[idx] != null ? ((Number) row[idx]).longValue() : null;
        idx++;

        String crtrDt = (String) row[idx++];

        Long newsTopicId = row[idx] != null ? ((Number) row[idx]).longValue() : null;
        idx++;

        String newsTopicName = (String) row[idx++];

        Integer newsSummaryId = row[idx] != null ? ((Number) row[idx]).intValue() : null;
        idx++;

        Integer dataRegistrationSn = row[idx] != null ? ((Number) row[idx]).intValue() : null;
        idx++;

        LocalDateTime firstCreated = row[idx] != null
            ? ((java.sql.Timestamp) row[idx]).toLocalDateTime()
            : null;
        idx++;

        String briefSummary = (String) row[idx++];

        String summaryStr = (String) row[idx++];
        List<String> summary = parseSummary(summaryStr);

        String title = (String) row[idx++];

        String provider = (String) row[idx++];

        String regionsStr = (String) row[idx];
        List<String> regions = parseRegions(regionsStr);

        return new NewsTopicListRes(
            newsTopicRelationshipId,
            crtrDt,
            newsTopicId,
            newsTopicName,
            newsSummaryId,
            dataRegistrationSn,
            firstCreated,
            briefSummary,
            summary,
            title,
            provider,
            regions
        );
    }

    /**
     * summary 문자열을 줄바꿈으로 분리하여 리스트로 변환
     *
     * @param summaryStr 줄바꿈으로 구분된 요약 문자열
     * @return 요약 문자열 리스트
     */
    private List<String> parseSummary(String summaryStr) {
        if (summaryStr == null || summaryStr.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(summaryStr.split("\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .toList();
    }

    /**
     * regions 문자열을 쉼표로 분리하여 리스트로 변환
     *
     * @param regionsStr 쉼표로 구분된 지역 코드 문자열
     * @return 지역 코드 리스트
     */
    private List<String> parseRegions(String regionsStr) {
        if (regionsStr == null || regionsStr.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(regionsStr.split(","))
            .map(String::trim)
            .filter(region -> !region.isEmpty())
            .distinct()
            .toList();
    }
}
