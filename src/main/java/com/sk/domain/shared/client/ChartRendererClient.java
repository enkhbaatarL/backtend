package com.sk.domain.shared.client;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import com.sk.common.properties.ChartCaptureProperties;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Playwright Java를 이용한 헤드리스 차트 이미지 캡처 클라이언트.
 * 프론트엔드가 전역 변수 window.__mailContents에 노출한 { htmlBody, images } 를 추출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChartRendererClient {

    private final ChartCaptureProperties properties;

    /**
     * 프론트엔드 차트 렌더 페이지를 헤드리스 Chrome으로 열고
     * 전역 변수 window.__mailContents 에서 htmlBody와 이미지 목록을 추출한다.
     *
     * @param renderUrl /newsletter-chart-render?token=JWT URL
     * @return htmlBody + CID별 base64 PNG 이미지 목록
     */
    public MailContents capture(String renderUrl) {
        log.info("Starting mail contents capture: {}", renderUrl);

        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(
                 new BrowserType.LaunchOptions()
                     .setHeadless(true)
                     .setArgs(List.of(
                         "--no-sandbox",
                         "--disable-setuid-sandbox",
                         "--disable-dev-shm-usage",  // Docker /dev/shm 64MB 제한 우회
                         "--disable-gpu"             // 헤드리스 환경 GPU 불필요
                     ))
             );
             BrowserContext context = browser.newContext()) {

            Page page = context.newPage();
            page.navigate(renderUrl, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.NETWORKIDLE));

            page.waitForSelector("body[data-charts-ready=\"true\"]",
                new Page.WaitForSelectorOptions()
                    .setTimeout(properties.getCaptureTimeoutMs()));

            @SuppressWarnings("unchecked")
            Map<String, Object> mailContents =
                (Map<String, Object>) page.evaluate("() => window.__mailContents");

            if (mailContents == null) {
                log.warn("window.__mailContents is null");
                return new MailContents("", List.of());
            }

            String htmlBody = (String) mailContents.get("htmlBody");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> rawImages =
                (List<Map<String, String>>) mailContents.get("images");

            List<CapturedImage> images = (rawImages == null) ? List.of() : rawImages.stream()
                .filter(img -> img != null && img.get("cid") != null)
                .map(img -> new CapturedImage(
                    img.get("cid"),
                    img.get("data"),
                    img.get("contentType")
                ))
                .toList();

            log.info("Mail contents captured: {} image(s)", images.size());
            return new MailContents(htmlBody, images);
        }
    }

    /** 프론트엔드 window.__mailContents 계약 */
    public record MailContents(String htmlBody, List<CapturedImage> images) {}

    /** 단일 인라인 이미지 (CID + base64 data + contentType) */
    public record CapturedImage(String cid, String data, String contentType) {}

}
