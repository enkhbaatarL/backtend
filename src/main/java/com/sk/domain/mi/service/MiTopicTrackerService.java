package com.sk.domain.mi.service;


import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.news.NewsTopicListRes;
import com.sk.domain.mi.dto.response.news.TopicInfoRes;
import com.sk.domain.mi.dto.response.news.TopicTrackerListRes;
import com.sk.domain.shared.entity.master.tmd.NewsTopic;
import com.sk.domain.shared.enums.topic.DateRangeOption;
import com.sk.domain.shared.enums.topic.SortType;
import com.sk.domain.shared.repository.master.tmd.NewsTopicRelationshipRepository;
import com.sk.domain.shared.repository.master.tmd.NewsTopicRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * Topic Tracker 목록 조회
     */
    public PaginationRes<TopicTrackerListRes> getTopicTrackerList(
        int page,
        int size,
        SortType sort,
        String keyword,
        DateRangeOption dateRangeOption,
        String topics,
        String regions
    ) {
        String sortDirection = (sort == SortType.OLDEST) ? "ASC" : "DESC";

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (dateRangeOption != DateRangeOption.ALL_TIME) {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = dateRangeOption.getStartDate(endDate);
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.atTime(LocalTime.MAX);
        }

        int offset = (page - 1) * size;
        List<Long> topicList = (topics != null && !topics.isBlank())
            ? Arrays.stream(topics.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(Long::parseLong)
            .toList()
            : Collections.emptyList();
        List<String> regionList = (regions != null && !regions.isBlank())
            ? Arrays.stream(regions.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList()
            : Collections.emptyList();

        boolean filterByTopic = !topicList.isEmpty();
        boolean filterByRegion = !regionList.isEmpty();

        List<Long> topicIdsParam = filterByTopic ? topicList : List.of(-1L);
        List<String> regionParam = filterByRegion ? regionList : List.of("DUMMY");

        List<Object[]> results = newsTopicRelationshipRepository.findTopicTrackerListWithRegions(
            keyword,
            startDateTime,
            endDateTime,
            topicIdsParam,
            filterByTopic,
            regionParam,
            filterByRegion,
            sortDirection,
            size,
            offset
        );

        long totalCount = newsTopicRelationshipRepository.countTopicTrackerList(
            keyword,
            startDateTime,
            endDateTime,
            topicIdsParam,
            filterByTopic,
            regionParam,
            filterByRegion
        );

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<TopicTrackerListRes> content = results.stream()
            .map(row -> {
                int idx = 0;
                Long newsTopicRelationshipId = row[idx] != null
                    ? ((Number) row[idx]).longValue()
                    : null;
                idx++;

                String criteriaDate = (String) row[idx++];

                Long newsSummaryId = row[idx] != null ? ((Number) row[idx]).longValue() : null;
                idx++;

                LocalDateTime firstCreated = row[idx] != null
                    ? ((java.sql.Timestamp) row[idx]).toLocalDateTime()
                    : null;
                idx++;

                String topicName = (String) row[idx++];
                Long topicId = row[idx] != null ? ((Number) row[idx]).longValue() : null;
                idx++;
                String providerName = (String) row[idx++];
                String summaryStr = (String) row[idx++];
                String briefSummary = (String) row[idx++];
                String briefSummaryEn = (String) row[idx++];
                String regionsStr = (String) row[idx];

                List<String> summary = summaryStr != null && !summaryStr.isBlank()
                    ? Arrays.asList(summaryStr.split("\n"))
                    : Collections.emptyList();

                List<String> regionItems = parseRegions(regionsStr);

                String publishDate = firstCreated != null ? firstCreated.format(dateFormatter) : null;
                String publishTime = firstCreated != null ? firstCreated.format(timeFormatter) : null;

                return new TopicTrackerListRes(
                    newsTopicRelationshipId,
                    newsSummaryId,
                    criteriaDate,
                    publishDate,
                    publishTime,
                    topicName,
                    topicId,
                    providerName,
                    regionItems,
                    briefSummary,
                    summary,
                    briefSummaryEn
                );
            })
            .toList();

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<TopicTrackerListRes> topicTrackerPage = new PageImpl<>(content, pageable, totalCount);

        return PaginationRes.toRes(topicTrackerPage);
    }

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
