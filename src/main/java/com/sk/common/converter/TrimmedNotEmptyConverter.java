package com.sk.common.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TrimmedNotEmptyConverter implements Converter<String, String> {
    @Override
    public String convert(String source) {
        return StringUtils.isEmpty(source) ? null : StringUtils.trim(source);
    }
}