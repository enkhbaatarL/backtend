package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.PolicyTacticsSeparateCd;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thymeleaf.util.StringUtils;

@Converter
public class StringToPolicyTacticsSeparateCdConverter implements AttributeConverter<PolicyTacticsSeparateCd, String> {

    @Override
    public String convertToDatabaseColumn(PolicyTacticsSeparateCd policyTacticsSeparateCd) {
        return policyTacticsSeparateCd.getCode();
    }

    @Override
    public PolicyTacticsSeparateCd convertToEntityAttribute(String s) {
        if (StringUtils.isEmpty(s)) {
            return PolicyTacticsSeparateCd.PRIVACY_POLICY;
        }
        return PolicyTacticsSeparateCd.of(s);
    }

}
