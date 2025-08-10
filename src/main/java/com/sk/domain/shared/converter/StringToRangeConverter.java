package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.chart.Range;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToRangeConverter implements Converter<String, Range> {

    @Override
    public Range convert(String source) {
        if (source.isBlank()) {
            return null;
        }
        return Range.findByCode(source);
    }

}
