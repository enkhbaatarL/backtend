package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.nextgen.SignpostStrength;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SignpostStrengthConverter implements AttributeConverter<SignpostStrength, String> {

    @Override
    public String convertToDatabaseColumn(SignpostStrength attribute) {
        // DB 적재 코드는 대문자 (VERYSTRONG/STRONG/MEDIUM/WEAK/VERYWEAK)
        return attribute == null ? null : attribute.name();
    }

    @Override
    public SignpostStrength convertToEntityAttribute(String dbData) {
        // 대소문자는 from()이 equalsIgnoreCase로 처리. 여기선 null/공백만 방어.
        return (dbData == null || dbData.isBlank()) ? null : SignpostStrength.from(dbData.trim());
    }

}
