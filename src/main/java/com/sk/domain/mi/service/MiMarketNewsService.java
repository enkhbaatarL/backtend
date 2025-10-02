package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.shared.entity.master.tmd.NewsSummary;
import com.sk.domain.shared.repository.master.tmd.NewsSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiMarketNewsService {

    private final NewsSummaryRepository newsSummaryRepository;

    /**
     * 시장 뉴스 목록 조회
     */
    public PaginationRes<MarketNewsListRes> getMarketNewsList(int page, int size,
                                                              String keyword) {

        // 페이지 설정 (최신순 정렬)
        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Direction.DESC, "firstCreated"));

        // 검색 조건에 따른 데이터 조회
        Page<NewsSummary> newsPage;

        newsPage = newsSummaryRepository.findAll(pageable);

        // DTO 변환
        Page<MarketNewsListRes> newsListPage = newsPage.map(this::toMarketNewsListRes);

        return PaginationRes.toRes(newsListPage);
    }

    /**
     * 시장 뉴스 상세 조회
     */
    public MarketNewsDetailRes getMarketNewsDetail(String newsSummaryId) {

        NewsSummary newsData = newsSummaryRepository.findById(Integer.parseInt(newsSummaryId))
            .orElseThrow(() -> AppErrorException.of(Error.RSC_DATA_NOT_FOUND,
                "뉴스를 찾을 수 없습니다. newsSummaryId: " + newsSummaryId));

        return toMarketNewsDetailRes(newsData);
    }

    /**
     * NewsSummary -> MarketNewsListRes 변환
     */
    private MarketNewsListRes toMarketNewsListRes(NewsSummary newsSummary) {

        return new MarketNewsListRes(
            newsSummary.getNewsSummaryId(),
            newsSummary.getRefinitivNewsData().getId().getDataRegistrationSn(),
            newsSummary.getRefinitivNewsData().getId().getFirstCreated(),
            newsSummary.getRefinitivNewsData().getTitle(),
            newsSummary.getRefinitivNewsData().getStory()
        );
    }

    /**
     * NewsSummary -> MarketNewsDetailRes 변환
     */
    private MarketNewsDetailRes toMarketNewsDetailRes(NewsSummary newsSummary) {
        return new MarketNewsDetailRes(
            newsSummary.getNewsSummaryId(),
            newsSummary.getRefinitivNewsData().getId().getDataRegistrationSn(),
            newsSummary.getRefinitivNewsData().getId().getFirstCreated(),
            newsSummary.getRefinitivNewsData().getTitle(),
            newsSummary.getRefinitivNewsData().getStory(),
            newsSummary.getProvider()
        );
    }
}
