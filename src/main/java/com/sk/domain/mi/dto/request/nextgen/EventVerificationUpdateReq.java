package com.sk.domain.mi.dto.request.nextgen;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

@JsonDeserialize(using = EventVerificationUpdateReq.Deserializer.class)
public record EventVerificationUpdateReq(
    @Schema(description = "화면에 표시한 해당 Event의 검증 결과 UUID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull UUID verificationId,
    @Schema(description = "이전 검증의 sourceUpdatedAt 원문. 정밀도·시간대 변환 없이 전달", example = "2026-11-06T09:00:00.123456")
    String expectedUpdatedAt
) {
    public static class Deserializer extends JsonDeserializer<EventVerificationUpdateReq> {
        @Override
        public EventVerificationUpdateReq deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode body = parser.readValueAsTree();
            if (!body.isObject() || !body.has("verificationId") || body.size() > 2
                || (body.size() == 2 && !body.has("expectedUpdatedAt"))) {
                return context.reportInputMismatch(EventVerificationUpdateReq.class, "verificationId is required; only expectedUpdatedAt may also be supplied");
            }
            JsonNode id = body.get("verificationId");
            if (!id.isTextual() || !id.textValue().matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                return context.reportInputMismatch(EventVerificationUpdateReq.class, "verificationId must be a UUID string");
            }
            UUID verificationId = UUID.fromString(id.textValue());
            JsonNode timestamp = body.get("expectedUpdatedAt");
            if (timestamp == null || timestamp.isNull()) return new EventVerificationUpdateReq(verificationId, null);
            if (!timestamp.isTextual()) {
                return context.reportInputMismatch(EventVerificationUpdateReq.class, "expectedUpdatedAt must be an ISO date-time string");
            }
            try {
                DateTimeFormatter.ISO_DATE_TIME.parse(timestamp.textValue());
            } catch (DateTimeParseException ex) {
                return context.reportInputMismatch(EventVerificationUpdateReq.class, "expectedUpdatedAt must be an ISO date-time string");
            }
            return new EventVerificationUpdateReq(verificationId, timestamp.textValue());
        }
    }
}
