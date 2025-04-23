package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.chart.Interval;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToIntervalConverter implements Converter<String, Interval> {

    @Override
    public Interval convert(String source) {
        if (source.isBlank()) {
            return null;
        }
        return Interval.findByCode(source);
    }

}
