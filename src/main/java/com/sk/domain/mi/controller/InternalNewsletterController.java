package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.newsletter.InternalNewsletterPublishReq;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailV2Res;
import com.sk.domain.mi.service.MiNewsletterService;
import com.sk.domain.mi.service.newsletter.NewsletterPublishPipelineService;
import com.sk.domain.shared.service.ChartCaptureJwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * chart-capture JWT로 보호되는 내부 전용 API
 * Playwright 서비스에서 프론트엔드 차트 렌더링에 필요한 데이터를 조회한다.
 */
@Tag(name = "19. Internal Newsletter Mail API", description = "Internal 뉴스레터 Mail API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/internal/newsletter")
@RequiredArgsConstructor
public class InternalNewsletterController {

    private final ChartCaptureJwtService chartCaptureJwtService;
    private final MiNewsletterService miNewsletterService;
    private final NewsletterPublishPipelineService newsletterPublishPipelineService;

    /**
     * 차트 렌더링용 뉴스레터 데이터 조회
     * Authorization: Bearer <chart-capture JWT>
     */
    @ApiOrder(3)
    @GetMapping("/render-data")
    public BaseResponse<NewsletterDetailV2Res> getChartData(
        @RequestHeader(value = "Authorization", required = false) String authorization) {

        String token = extractBearerToken(authorization);
        ChartCaptureJwtService.ChartCaptureTokenClaims chartContext;
        try {
            chartContext = chartCaptureJwtService.validateAndExtractChartContext(token);
        } catch (IllegalArgumentException e) {
            log.warn("chart-capture token validation failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        NewsletterDetailV2Res detail = miNewsletterService.getNewsletterDetailForSystem(
            chartContext.newsletterId(),
            chartContext.issueNo()
        );

        return BaseResponseFactory.create(detail);
    }

    @ApiOrder(4)
    @PostMapping("/publish")
    public BaseResponse<Boolean> publish(
        @RequestBody InternalNewsletterPublishReq request,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String token = extractBearerToken(authorization);
        try {
            chartCaptureJwtService.validateAndExtract(token);
        } catch (IllegalArgumentException e) {
            log.warn("chart-capture token validation failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        newsletterPublishPipelineService.executePublish(
            request.newsletterMasterId(),
            request.issueNo(),
            request.publishHistoryId()
        );

        return BaseResponseFactory.create(Boolean.TRUE);
    }

    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
    }

}
