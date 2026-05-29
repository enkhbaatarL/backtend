package com.sk.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .registerModule(new JavaTimeModule());

    private JsonUtil() {}

    private static class JsonUtilHolder {
        private static final JsonUtil INSTANCE = new JsonUtil();
    }

    // Get the singleton instance
    public static JsonUtil getInstance() {
        return JsonUtilHolder.INSTANCE;
    }

    // Convert an object to JSON string
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // Convert a JSON string to an object of the specified class
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // Convert a JSON string to an JsonNode
    public JsonNode fromJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // 단일 키로 문자열 배열을 감싼 JSON에서 해당 배열을 추출
    public static List<String> parseWrappedStringList(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JsonNode node = objectMapper.readTree(json).get(fieldName);
            if (node == null || !node.isArray()) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<>(node.size());
            node.forEach(item -> result.add(item.asText()));
            return result;
        } catch (Exception e) {
            log.warn("JSON wrapped string list 파싱 실패 (field={}): {}", fieldName, e.getMessage());
            return Collections.emptyList();
        }
    }

    public static ObjectNode createNode() {
        return objectMapper.createObjectNode();
    }

    public static ObjectMapper getMapper() {
        return objectMapper;
    }

}
