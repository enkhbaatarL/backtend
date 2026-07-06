package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.nextgen.PriceInterpretationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PriceInterpretationStatusConverter implements AttributeConverter<PriceInterpretationStatus, String> {

    @Override
    public String convertToDatabaseColumn(PriceInterpretationStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public PriceInterpretationStatus convertToEntityAttribute(String dbData) {
        // 대소문자는 from()이 equalsIgnoreCase로 처리. 여기선 null/공백만 방어.
        return (dbData == null || dbData.isBlank()) ? null : PriceInterpretationStatus.from(dbData.trim());
    }

}
