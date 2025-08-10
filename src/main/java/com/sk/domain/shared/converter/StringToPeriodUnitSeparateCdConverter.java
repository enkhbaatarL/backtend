package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thymeleaf.util.StringUtils;

@Converter
public class StringToPeriodUnitSeparateCdConverter implements AttributeConverter<PeriodUnitSeparateCd, String> {

    @Override
    public String convertToDatabaseColumn(PeriodUnitSeparateCd periodUnitSeparateCd) {
        return periodUnitSeparateCd.getCode();
    }

    @Override
    public PeriodUnitSeparateCd convertToEntityAttribute(String s) {
        if (StringUtils.isEmpty(s)) {
            return PeriodUnitSeparateCd.D;
        }
        return PeriodUnitSeparateCd.valueOf(s);
    }

}
