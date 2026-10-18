package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 이 API에서만 사용하는 제한/정확한 수치 직렬화. 전역 mapper를 바꾸지 않는다. */
@Component
public class InputDraftJson {
    public static final int BODY_LIMIT=1_048_576;
    public static final int RESPONSE_LIMIT=2_097_152;
    private final ObjectMapper mapper;
    public InputDraftJson(ObjectMapper configured) {
        mapper=configured.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
            DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        mapper.getFactory().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        mapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
            .maxNestingDepth(16).maxStringLength(BODY_LIMIT).maxNumberLength(4096).build());
        mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
    }
    public String sourceVersion(InputStream input) {
        try {
            byte[] bytes=input.readNBytes(BODY_LIMIT+1);
            if(bytes.length>BODY_LIMIT) throw InputDraftException.large();
            JsonNode node=mapper.readTree(bytes);
            if(node==null || !node.isObject() || node.size()!=1 || !node.has("sourceVersion")
                || !node.get("sourceVersion").isTextual()) throw InputDraftException.invalid();
            String value=node.get("sourceVersion").textValue();
            if(!value.matches("sv1_[0-9a-f]{64}")) throw InputDraftException.invalid();
            return value;
        } catch(IOException ex) { throw InputDraftException.json(); }
    }
    public JsonNode definition(String text) {
        if(text==null) throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        if(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>BODY_LIMIT) throw InputDraftException.internal();
        try { return mapper.readTree(text); } catch(IOException ex) { throw InputDraftException.conflict("POLICY_INCOMPLETE",null); }
    }
    public byte[] bytes(Object value) {
        try { return mapper.writeValueAsBytes(value); } catch(IOException ex) { throw InputDraftException.internal(); }
    }
    public JsonNode tree(Object value) { return mapper.valueToTree(value); }
    public static void keys(JsonNode node, Set<String> allowed) {
        if(!node.isObject()) throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        node.fieldNames().forEachRemaining(key->{ if(!allowed.contains(key)) throw InputDraftException.conflict("POLICY_INCOMPLETE",null); });
    }
}
