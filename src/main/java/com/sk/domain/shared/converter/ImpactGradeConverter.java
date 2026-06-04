package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.nextgen.ImpactGrade;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ImpactGradeConverter implements AttributeConverter<ImpactGrade, String> {

    @Override
    public String convertToDatabaseColumn(ImpactGrade attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ImpactGrade convertToEntityAttribute(String dbData) {
        return ImpactGrade.fromCode(dbData);
    }

}
