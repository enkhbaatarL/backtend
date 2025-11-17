package com.sk.common.converter;

import com.sk.domain.shared.enums.SignificanceLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * SignificanceLevel Enum을 DB의 tinyint(Byte)로 변환하는 Converter
 */
@Converter(autoApply = false)
public class SignificanceLevelAttributeConverter implements AttributeConverter<SignificanceLevel, Byte> {

    /**
     * Enum을 DB 값(Byte)으로 변환
     */
    @Override
    public Byte convertToDatabaseColumn(SignificanceLevel attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    /**
     * DB 값(Byte)을 Enum으로 변환
     */
    @Override
    public SignificanceLevel convertToEntityAttribute(Byte dbData) {
        if (dbData == null) {
            return null;
        }
        return SignificanceLevel.fromCode(dbData);
    }
}
