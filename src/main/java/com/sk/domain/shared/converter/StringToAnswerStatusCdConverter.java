package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.agent.AnswerStatusCd;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thymeleaf.util.StringUtils;

@Converter
public class StringToAnswerStatusCdConverter implements AttributeConverter<AnswerStatusCd, String> {

    @Override
    public String convertToDatabaseColumn(AnswerStatusCd answerStatusCd) {
        return answerStatusCd.getCode();
    }

    @Override
    public AnswerStatusCd convertToEntityAttribute(String s) {
        if (StringUtils.isEmpty(s)) {
            return AnswerStatusCd.FAIL;
        }
        return AnswerStatusCd.of(s);
    }

}
