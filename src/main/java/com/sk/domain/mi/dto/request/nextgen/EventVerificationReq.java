package com.sk.domain.mi.dto.request.nextgen;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

@JsonDeserialize(using = EventVerificationReq.Deserializer.class)
public record EventVerificationReq(
    @Schema(description = "검증할 Event ID 전체 목록. 양의 정수 1~5개, 중복 및 추가 필드 불가", example = "[5,12]")
    @NotEmpty @Size(max = 5) @UniqueElements List<@NotNull @Positive Long> miEventIds
) {
    // 애플리케이션 전역 Jackson의 문자열/소수 변환 및 unknown-field 무시 정책과 분리한다.
    public static class Deserializer extends JsonDeserializer<EventVerificationReq> {
        @Override
        public EventVerificationReq deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode body = parser.readValueAsTree();
            if (!body.isObject() || body.size() != 1 || !body.has("miEventIds") || !body.get("miEventIds").isArray()) {
                return context.reportInputMismatch(EventVerificationReq.class, "Only the miEventIds integer array is allowed");
            }
            var ids = new ArrayList<Long>();
            for (JsonNode id : body.get("miEventIds")) {
                if (!id.isIntegralNumber() || !id.canConvertToLong()) {
                    return context.reportInputMismatch(EventVerificationReq.class, "miEventIds must contain JSON integers within the Long range");
                }
                ids.add(id.longValue());
            }
            return new EventVerificationReq(List.copyOf(ids));
        }
    }
}
