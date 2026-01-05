package com.sk.domain.mi.service;


import com.sk.domain.mi.dto.response.news.TopicInfoRes;
import com.sk.domain.shared.entity.master.tmd.NewsTopic;
import com.sk.domain.shared.repository.master.tmd.NewsTopicRepository;
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

    /**
     * 활성화된 토픽 목록 조회
     * - del_yn = 'N', use_yn = 'Y' 조건
     * - 토픽 ID 기준 오름차순 정렬
     *
     * @return 토픽 정보 목록
     */
    public List<TopicInfoRes> getTopicList(int userNo) {
        log.debug("활성화된 토픽 목록 조회 시작");

        List<NewsTopic> activeTopics = newsTopicRepository.findActiveDefaultAndUserTopics("Y", userNo);

        List<TopicInfoRes> result = activeTopics.stream()
            .map(this::toTopicInfoRes)
            .toList();

        log.debug("활성화된 토픽 {} 건 조회 완료", result.size());

        return result;
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
            newsTopic.getNewsTopicName()
        );
    }
}
