package com.sk.domain.mi.dto.request.nextgen;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record EventRecommendationReq(
    @Schema(description = "선택한 Event ID 전체 목록 (양의 정수 1~5개, 중복 불가)", example = "[5, 12]")
    @NotEmpty @Size(max = 5) @UniqueElements
    @JsonDeserialize(contentUsing = StrictEventIdDeserializer.class)
    List<@NotNull @Positive Long> miEventIds
) {
    // Jackson의 기본 변환으로 1.5 또는 "1"이 유효한 Event ID가 되는 것을 방지한다.
    public static class StrictEventIdDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return (Long) context.handleUnexpectedToken(Long.class, parser);
            }
            return parser.getLongValue();
        }
    }
}
