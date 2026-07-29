package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.NewsletterText;
import com.sk.domain.shared.repository.master.tmd.NewsletterTextRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TemplateFieldValueResolver {

    private static final String MIME_TYPE = "image/png";
    private static final String FALLBACK_IMAGE_PATH = "static/images/oil_trend_cover_img.png";
    private static final DateTimeFormatter PUBLISH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String BRENT_PRICE_TEXT_TYPE = "SUB_INFO_TXT";
    private static final String META_KEY_DEFAULT_VALUE = "defaultValue";
    private static final Pattern BRENT_PRICE_RANGE_PATTERN = Pattern.compile(
        ".*?\\$\\s*(\\d+(?:\\.\\d+)?)\\s*/B\\s*(?:→|->)\\s*\\$\\s*(\\d+(?:\\.\\d+)?)\\s*/B.*"
    );

    private final ObjectMapper objectMapper;
    private final NewsletterTextRepository newsletterTextRepository;

    @Autowired
    public TemplateFieldValueResolver(
        ObjectMapper objectMapper,
        NewsletterTextRepository newsletterTextRepository
    ) {
        this.objectMapper = objectMapper;
        this.newsletterTextRepository = newsletterTextRepository;
    }

    TemplateFieldValueResolver(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    public Object resolveBlockFieldValue(
        TemplateTree.BlockNode blockNode,
        TemplateTree.FieldNode fieldNode,
        NewsletterPublishHistory pubHist
    ) {
        return resolveFieldValue(
            fieldNode.fieldKey(),
            fieldNode.fieldMeta(),
            new ResolveContext(pubHist, blockNode, null, null),
            true
        );
    }

    public Object resolveSectionFieldValue(
        TemplateTree.SectionNode sectionNode,
        TemplateTree.SectionFieldNode fieldNode,
        Map<String, ResolvedBlockData> resolvedData,
        NewsletterPublishHistory pubHist
    ) {
        return resolveFieldValue(
            fieldNode.fieldKey(),
            fieldNode.fieldMeta(),
            new ResolveContext(pubHist, null, sectionNode, resolvedData),
            false
        );
    }

    private Object resolveFieldValue(
        String fieldKey,
        String fieldMeta,
        ResolveContext context,
        boolean allowLegacyBlockFallback
    ) {
        FieldMetaSpec metaSpec = parseFieldMeta(fieldMeta);

        Object sourceDerivedValue = resolveValueSource(fieldKey, metaSpec, context);
        if (sourceDerivedValue != null) {
            return sourceDerivedValue;
        }

        if (allowLegacyBlockFallback && context.blockNode() != null) {
            sourceDerivedValue = resolveLegacyBlockSourceDerivedValue(context.blockNode(), fieldKey, context.pubHist());
            if (sourceDerivedValue != null) {
                return sourceDerivedValue;
            }
        }

        return metaSpec.defaultValue();
    }

    private Object resolveValueSource(
        String fieldKey,
        FieldMetaSpec metaSpec,
        ResolveContext context
    ) {
        ValueSourceSpec valueSource = metaSpec.valueSource();
        if (valueSource == null || StringUtils.isBlank(valueSource.srcTypeCd())) {
            return null;
        }

        NewsletterPublishHistory pubHist = context.pubHist();
        NewsletterMaster master = pubHist != null ? pubHist.getNewsletterMaster() : null;

        return switch (valueSource.srcTypeCd()) {
            case "NEWSLETTER_MASTER_NAME" -> master != null ? master.getMasterName() : null;
            case "NEWSLETTER_MASTER_EXPLANATION" -> master != null ? master.getExplanation() : null;
            case "NEWSLETTER_MASTER_PERIOD_TYPE" -> master != null && master.getPeriodType() != null
                ? master.getPeriodType().getCode() : null;
            case "NEWSLETTER_MASTER_IMAGE_URL" -> encodeImageToBase64(master != null ? master.getImageUrl() : null);
            case "NEWSLETTER_PUBLISH_HISTORY_PUBLISH_DTM" -> pubHist != null && pubHist.getPublishDateTime() != null
                ? pubHist.getPublishDateTime().format(PUBLISH_DATE_FORMATTER) : null;
            case "NEWSLETTER_PUBLISH_HISTORY_ISSUE_NO" -> pubHist != null ? pubHist.getIssueNo() : null;
            case "NEWSLETTER_PUBLISH_HISTORY_TITLE" -> pubHist != null ? pubHist.getNewsletterTitle() : null;
            case "NEWSLETTER_PUBLISH_HISTORY_AI_SUMMARY" -> pubHist != null ? pubHist.getAiSummary() : null;
            case "PRICE_CHANGE_TO" -> resolveBrentPriceValue(master, pubHist, true);
            case "PRICE_CHANGE_FROM" -> resolveBrentPriceValue(master, pubHist, false);
            case "NEWSLETTER_MASTER" -> resolveNewsletterMasterValue(master, valueSource, metaSpec);
            case "PUBLISH_CONTEXT" -> resolvePublishContextValue(pubHist, valueSource, metaSpec);
            case "BLOCK_CONTENT" -> resolveBlockContentValue(fieldKey, valueSource, context);
            case "LITERAL_DEFAULT", "SNAPSHOT_FIXED", "EXTERNAL_DOMAIN" -> null;
            default -> null;
        };
    }

    private Object resolveBrentPriceValue(
        NewsletterMaster master,
        NewsletterPublishHistory pubHist,
        boolean isPriceChangeTo
    ) {
        if (master == null || pubHist == null || newsletterTextRepository == null) {
            return null;
        }

        String newsletterTextValue = newsletterTextRepository
            .findByNewsletterPublishHistoryAndNewsletterTextType(pubHist, BRENT_PRICE_TEXT_TYPE)
            .map(NewsletterText::getNewsletterTextValue)
            .orElse(null);
        if (StringUtils.isBlank(newsletterTextValue)) {
            return null;
        }

        Matcher matcher = BRENT_PRICE_RANGE_PATTERN.matcher(newsletterTextValue);
        if (!matcher.matches()) {
            return null;
        }

        try {
            BigDecimal priceChangeFrom = new BigDecimal(matcher.group(1));
            BigDecimal priceChangeTo = new BigDecimal(matcher.group(2));
            return isPriceChangeTo ? priceChangeTo : priceChangeFrom;
        } catch (NumberFormatException e) {
            log.warn("브렌트 가격 문자열 파싱 실패 - nws_ltr_txt_value: {}", newsletterTextValue);
            return null;
        }
    }

    private Object resolveNewsletterMasterValue(
        NewsletterMaster master,
        ValueSourceSpec valueSource,
        FieldMetaSpec metaSpec
    ) {
        if (master == null || StringUtils.isBlank(valueSource.srcRef())) {
            return null;
        }

        Object rawValue = switch (valueSource.srcRef()) {
            case "masterName" -> master.getMasterName();
            case "explanation" -> master.getExplanation();
            case "periodType", "periodTypeCode" -> master.getPeriodType();
            case "imageUrl" -> master.getImageUrl();
            default -> null;
        };
        return applyTransform(rawValue, metaSpec);
    }

    private Object resolvePublishContextValue(
        NewsletterPublishHistory pubHist,
        ValueSourceSpec valueSource,
        FieldMetaSpec metaSpec
    ) {
        if (pubHist == null || StringUtils.isBlank(valueSource.srcRef())) {
            return null;
        }

        Object rawValue = switch (valueSource.srcRef()) {
            case "publishDateTime", "publishDtm" -> pubHist.getPublishDateTime();
            case "issueNo" -> pubHist.getIssueNo();
            case "newsletterTitle", "title" -> pubHist.getNewsletterTitle();
            case "aiSummary" -> pubHist.getAiSummary();
            default -> null;
        };
        return applyTransform(rawValue, metaSpec);
    }

    private Object resolveBlockContentValue(
        String fieldKey,
        ValueSourceSpec valueSource,
        ResolveContext context
    ) {
        if (context.sectionNode() == null || context.resolvedData() == null || StringUtils.isBlank(valueSource.srcRef())) {
            return null;
        }

        TemplateTree.BlockNode sourceBlock = context.sectionNode().blocks().stream()
            .filter(block -> valueSource.srcRef().equals(block.blockCode()))
            .findFirst()
            .orElse(null);
        if (sourceBlock == null) {
            return null;
        }

        Map<String, Object> content = context.resolvedData()
            .getOrDefault(BlockDataResolver.resolvedDataKey(sourceBlock.id()), ResolvedBlockData.of(Map.of()))
            .toContentMap();
        if (content.isEmpty()) {
            return null;
        }

        String contentFieldKey = valueSource.contentFieldKey();
        if (StringUtils.isBlank(contentFieldKey)) {
            contentFieldKey = fieldKey;
        }
        return content.get(contentFieldKey);
    }

    private Object resolveLegacyBlockSourceDerivedValue(
        TemplateTree.BlockNode block,
        String fieldKey,
        NewsletterPublishHistory pubHist
    ) {
        NewsletterMaster master = pubHist != null ? pubHist.getNewsletterMaster() : null;
        return switch (block.blockCode()) {
            case "COVER_IMAGE_CARD" -> {
                if (!"imageBase64".equals(fieldKey)) {
                    yield null;
                }
                yield encodeImageToBase64(master != null ? master.getImageUrl() : null);
            }
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

    private Object applyTransform(Object rawValue, FieldMetaSpec metaSpec) {
        if (rawValue == null) {
            return null;
        }
        if (StringUtils.isBlank(metaSpec.transform())) {
            return rawValue;
        }

        return switch (metaSpec.transform()) {
            case "ENUM_CODE" -> rawValue instanceof Enum<?> enumValue ? enumValue.name() : rawValue;
            case "FORMAT_DATE" -> rawValue instanceof java.time.temporal.TemporalAccessor temporalAccessor
                ? PUBLISH_DATE_FORMATTER.format(temporalAccessor) : rawValue;
            case "ENCODE_IMAGE_BASE64" -> rawValue instanceof String stringValue ? encodeImageToBase64(stringValue) : null;
            case "FORMAT_DAILY_RANGE_KST" -> rawValue instanceof LocalDateTime dateTime
                ? formatDailyRangeKst(dateTime) : rawValue;
            default -> rawValue;
        };
    }

    private String formatDailyRangeKst(LocalDateTime publishDateTime) {
        DateTimeFormatter rangeFormatter = DateTimeFormatter.ofPattern("MMM dd HH:mm", Locale.US);
        LocalDateTime rangeStart = publishDateTime.minusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime rangeEnd = publishDateTime.withHour(9).withMinute(0).withSecond(0).withNano(0);
        return rangeStart.format(rangeFormatter) + " - " + rangeEnd.format(rangeFormatter) + " (KST)";
    }

    private FieldMetaSpec parseFieldMeta(String fieldMeta) {
        if (StringUtils.isBlank(fieldMeta)) {
            return FieldMetaSpec.empty();
        }

        try {
            JsonNode metaNode = objectMapper.readTree(fieldMeta);
            if (!metaNode.isObject()) {
                return FieldMetaSpec.empty();
            }

            Object defaultValue = metaNode.has(META_KEY_DEFAULT_VALUE) && !metaNode.get(META_KEY_DEFAULT_VALUE).isNull()
                ? objectMapper.convertValue(metaNode.get(META_KEY_DEFAULT_VALUE), Object.class)
                : null;
            ValueSourceSpec valueSource = parseValueSource(metaNode.get("valueSource"));
            String transform = readText(metaNode.get("transform"));
            String format = readText(metaNode.get("format"));

            if (valueSource != null) {
                if (StringUtils.isBlank(transform)) {
                    transform = valueSource.transform();
                }
                if (StringUtils.isBlank(format)) {
                    format = valueSource.format();
                }
            }

            return new FieldMetaSpec(defaultValue, valueSource, transform, format);
        } catch (Exception e) {
            return FieldMetaSpec.empty();
        }
    }

    private ValueSourceSpec parseValueSource(JsonNode valueSourceNode) {
        if (valueSourceNode == null || valueSourceNode.isNull()) {
            return null;
        }

        if (valueSourceNode.isTextual()) {
            return new ValueSourceSpec(valueSourceNode.asText(), null, null, null, null);
        }

        if (!valueSourceNode.isObject()) {
            return null;
        }

        return new ValueSourceSpec(
            readText(valueSourceNode.get("srcTypeCd")),
            readText(valueSourceNode.get("srcRef")),
            valueSourceNode.get("srcSpecJson"),
            readText(valueSourceNode.get("transform")),
            readText(valueSourceNode.get("format"))
        );
    }

    private String readText(JsonNode node) {
        return node != null && !node.isNull() ? node.asText() : null;
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
        // 26.07.29 취약점 진단 검토, '표준 라이브러리의 널 반환 값 역참조-102146', 오탐 -> ClassPathResource.getInputStream()은 리소스 미존재 시 FileNotFoundException을 던지며 null을 반환하지 않는 계약.
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] imageBytes = inputStream.readAllBytes();
            return "data:" + MIME_TYPE + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        }
    }

    private record ResolveContext(
        NewsletterPublishHistory pubHist,
        TemplateTree.BlockNode blockNode,
        TemplateTree.SectionNode sectionNode,
        Map<String, ResolvedBlockData> resolvedData
    ) {
    }

    private record FieldMetaSpec(
        Object defaultValue,
        ValueSourceSpec valueSource,
        String transform,
        String format
    ) {
        private static FieldMetaSpec empty() {
            return new FieldMetaSpec(null, null, null, null);
        }
    }

    private record ValueSourceSpec(
        String srcTypeCd,
        String srcRef,
        JsonNode srcSpecJson,
        String transform,
        String format
    ) {
        private String contentFieldKey() {
            if (srcSpecJson == null || srcSpecJson.isNull()) {
                return null;
            }
            JsonNode fieldKeyNode = srcSpecJson.get("fieldKey");
            return fieldKeyNode != null && !fieldKeyNode.isNull() ? fieldKeyNode.asText() : null;
        }
    }
}
