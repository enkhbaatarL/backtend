package com.sk.common.converter;

import com.fasterxml.jackson.annotation.JsonValue;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

/**
 * {@code @JsonValue}가 선언된 Enum의 code 값 또는 name을 대소문자 구분 없이 변환하는 ConverterFactory.
 *
 * Spring MVC의 {@code @RequestParam}, {@code @PathVariable}, {@code @ModelAttribute}, {@code @ParameterObject} 등 쿼리 파라미터 / 경로 변수 바인딩에만 적용됩니다.
 */
@Component
public class JsonValueEnumConverterFactory implements ConverterFactory<String, Enum<?>> {

    private final Map<Class<?>, Converter<String, ? extends Enum<?>>> converterCache =
        new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        return (Converter<String, T>) converterCache.computeIfAbsent(
            targetType, this::createConverter
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<?>> Converter<String, T> createConverter(Class<?> targetType) {
        Field jsonValueField = findJsonValueField(targetType);

        return source -> {
            if (source == null || source.isBlank()) {
                return null;
            }

            for (Enum<?> constant : ((Class<Enum<?>>) targetType).getEnumConstants()) {
                // @JsonValue code 매칭 (대소문자 무시)
                if (jsonValueField != null) {
                    try {
                        Object code = jsonValueField.get(constant);
                        if (code != null && source.equalsIgnoreCase(code.toString())) {
                            return (T) constant;
                        }
                    } catch (IllegalAccessException e) {
                        // fallback to name matching
                    }
                }

                // enum name 매칭 (대소문자 무시)
                if (source.equalsIgnoreCase(constant.name())) {
                    return (T) constant;
                }
            }

            throw new IllegalArgumentException("No enum constant '%s' in %s".formatted(source, targetType.getSimpleName()));
        };
    }

    private Field findJsonValueField(Class<?> enumType) {
        for (Field field : enumType.getDeclaredFields()) {
            if (field.isAnnotationPresent(JsonValue.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

}
