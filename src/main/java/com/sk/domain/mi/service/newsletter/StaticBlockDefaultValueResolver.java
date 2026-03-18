package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaticBlockDefaultValueResolver {

    private static final String MIME_TYPE = "image/png";
    private static final String FALLBACK_IMAGE_PATH = "static/images/oil_trend_cover_img.png";
    private static final DateTimeFormatter PUBLISH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper;

    public Map<String, Object> resolve(TemplateTree.BlockNode block, NewsletterPublishHistory pubHist) {
        Map<String, Object> content = new LinkedHashMap<>();
        NewsletterMaster master = pubHist != null ? pubHist.getNewsletterMaster() : null;

        for (TemplateTree.FieldNode field : block.fields()) {
            Object sourceDerivedValue = resolveSourceDerivedValue(block, field.fieldKey(), master, pubHist);
            if (sourceDerivedValue != null) {
                content.put(field.fieldKey(), sourceDerivedValue);
                continue;
            }

            Object templateDefault = extractTemplateDefault(field.fieldMeta());
            if (templateDefault != null) {
                content.put(field.fieldKey(), templateDefault);
            }
        }

        return content;
    }

    private Object resolveSourceDerivedValue(
        TemplateTree.BlockNode block,
        String fieldKey,
        NewsletterMaster master,
        NewsletterPublishHistory pubHist
    ) {
        return switch (block.blockCode()) {
            case "COVER_IMAGE_CARD" -> "imageBase64".equals(fieldKey) ? encodeImageToBase64(master != null ? master.getImageUrl() : null) : null;
            case "COVER_INFO" -> resolveCoverInfoField(fieldKey, master);
            case "DATE_ISSUE_INFO" -> resolveDateIssueField(fieldKey, pubHist);
            case "TITLE_MAIN_SPECIALIZED" -> "title".equals(fieldKey) && pubHist != null ? pubHist.getNewsletterTitle() : null;
            case "SUMMARY_BODY" -> "summaryText".equals(fieldKey) && pubHist != null ? pubHist.getAiSummary() : null;
            default -> null;
        };
    }

    private Object resolveCoverInfoField(String fieldKey, NewsletterMaster master) {
        if (master == null) {
            return null;
        }
        return switch (fieldKey) {
            case "publishCycle" -> master.getPeriodType() != null ? master.getPeriodType().getCode() : null;
            case "newsletterName" -> master.getMasterName();
            case "newsletterDescription" -> master.getExplanation();
            default -> null;
        };
    }

    private Object resolveDateIssueField(String fieldKey, NewsletterPublishHistory pubHist) {
        if (pubHist == null) {
            return null;
        }
        return switch (fieldKey) {
            case "publishDate" -> pubHist.getPublishDateTime() != null ? pubHist.getPublishDateTime().format(PUBLISH_DATE_FORMATTER) : null;
            case "issueNumber" -> pubHist.getIssueNo();
            default -> null;
        };
    }

    private Object extractTemplateDefault(String fieldMeta) {
        if (StringUtils.isBlank(fieldMeta)) {
            return null;
        }

        try {
            JsonNode metaNode = objectMapper.readTree(fieldMeta);
            JsonNode defaultValue = metaNode.get("defaultValue");
            if (defaultValue == null || defaultValue.isNull()) {
                return null;
            }
            return objectMapper.convertValue(defaultValue, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String encodeImageToBase64(String imagePath) {
        String normalizedImagePath = StringUtils.defaultIfBlank(imagePath, FALLBACK_IMAGE_PATH);
        try {
            return readImageAsBase64(normalizedImagePath);
        } catch (IOException e) {
            if (!FALLBACK_IMAGE_PATH.equals(normalizedImagePath)) {
                try {
                    return readImageAsBase64(FALLBACK_IMAGE_PATH);
                } catch (IOException fallbackException) {
                    log.error("기본 뉴스레터 이미지를 Base64로 변환하지 못했습니다.", fallbackException);
                }
            } else {
                log.error("뉴스레터 이미지를 Base64로 변환하지 못했습니다. imagePath={}", imagePath, e);
            }
            return null;
        }
    }

    private String readImageAsBase64(String imagePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(imagePath);
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] imageBytes = inputStream.readAllBytes();
            return "data:" + MIME_TYPE + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        }
    }
}
