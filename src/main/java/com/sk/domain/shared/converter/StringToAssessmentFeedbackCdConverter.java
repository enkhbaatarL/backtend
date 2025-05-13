package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.agent.AssessmentFeedbackCd;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thymeleaf.util.StringUtils;

@Converter
public class StringToAssessmentFeedbackCdConverter implements AttributeConverter<AssessmentFeedbackCd, String> {

    @Override
    public String convertToDatabaseColumn(AssessmentFeedbackCd assessmentFeedbackCd) {
        return assessmentFeedbackCd.getCode();
    }

    @Override
    public AssessmentFeedbackCd convertToEntityAttribute(String s) {
        if (StringUtils.isEmpty(s)) {
            return null;
        }
        return AssessmentFeedbackCd.of(s);
    }

}
