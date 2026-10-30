package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Component;

/** 이 API에서만 사용하는 제한/정확한 수치 직렬화. 전역 mapper를 바꾸지 않는다. */
@Component
public class InputDraftJson {
    public static final int BODY_LIMIT=1_048_576;
    public static final int RESPONSE_LIMIT=2_097_152;
    public static final int DELTA_LIMIT=16_777_216;
    private final ObjectMapper mapper;
    private final ObjectWriter changeWriter;
    public InputDraftJson(ObjectMapper configured) {
        mapper=configured.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
            DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        mapper.getFactory().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        mapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
            .maxNestingDepth(16).maxStringLength(BODY_LIMIT).maxNumberLength(4096).build());
        mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        // 성공 이력의 큰 지수도 기존 plain 표시 계약으로 정확히 반환한다.
        // writeNumber(String)은 BigDecimal writer의 scale ±9999 제한을 거치지 않는다.
        var numbers=new com.fasterxml.jackson.databind.module.SimpleModule();
        numbers.addSerializer(BigDecimal.class,new com.fasterxml.jackson.databind.JsonSerializer<BigDecimal>() {
            @Override public void serialize(BigDecimal value,JsonGenerator out,com.fasterxml.jackson.databind.SerializerProvider provider)throws IOException {
                if((long)value.precision()+Math.abs((long)value.scale())+2>DELTA_LIMIT)throw new IOException("number limit");
                out.writeNumber(value.toPlainString());
            }
        });
        numbers.addSerializer(com.fasterxml.jackson.databind.node.DecimalNode.class,new com.fasterxml.jackson.databind.JsonSerializer<com.fasterxml.jackson.databind.node.DecimalNode>() {
            @Override public void serialize(com.fasterxml.jackson.databind.node.DecimalNode value,JsonGenerator out,com.fasterxml.jackson.databind.SerializerProvider provider)throws IOException {
                BigDecimal number=value.decimalValue();
                if((long)number.precision()+Math.abs((long)number.scale())+2>DELTA_LIMIT)throw new IOException("number limit");
                out.writeNumber(number.toPlainString());
            }
        });
        // controls 등 JsonNode 안의 숫자도 같은 정확한 plain writer를 사용한다.
        numbers.addSerializer(ObjectNode.class,new com.fasterxml.jackson.databind.JsonSerializer<ObjectNode>() {
            @Override public void serialize(ObjectNode value,JsonGenerator out,com.fasterxml.jackson.databind.SerializerProvider provider)throws IOException {
                out.writeStartObject();var fields=value.fields();while(fields.hasNext()){var field=fields.next();out.writeFieldName(field.getKey());provider.defaultSerializeValue(field.getValue(),out);}out.writeEndObject();
            }
        });
        numbers.addSerializer(ArrayNode.class,new com.fasterxml.jackson.databind.JsonSerializer<ArrayNode>() {
            @Override public void serialize(ArrayNode value,JsonGenerator out,com.fasterxml.jackson.databind.SerializerProvider provider)throws IOException {
                out.writeStartArray();for(JsonNode child:value)provider.defaultSerializeValue(child,out);out.writeEndArray();
            }
        });
        mapper.registerModule(numbers);
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
        // 요청/성공 delta 저장은 지수 표기를 허용한다. 조회 응답은 기존 plain 숫자 계약을 유지한다.
        ObjectMapper stored=mapper.copy();
        var storedNumbers=new com.fasterxml.jackson.databind.module.SimpleModule();
        storedNumbers.addSerializer(BigDecimal.class,new com.fasterxml.jackson.databind.JsonSerializer<BigDecimal>() {
            @Override public void serialize(BigDecimal value,JsonGenerator out,com.fasterxml.jackson.databind.SerializerProvider provider)throws IOException {out.writeNumber(value);}
        });
        storedNumbers.addSerializer(com.fasterxml.jackson.databind.node.DecimalNode.class,new com.fasterxml.jackson.databind.JsonSerializer<com.fasterxml.jackson.databind.node.DecimalNode>() {
            @Override public void serialize(com.fasterxml.jackson.databind.node.DecimalNode value,JsonGenerator out,com.fasterxml.jackson.databind.SerializerProvider provider)throws IOException {out.writeNumber(value.decimalValue());}
        });
        stored.registerModule(storedNumbers);
        changeWriter=stored.writer().without(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
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
    public record FinalizationRequest(String requestId,long baseRevision,String title,String description) {}
    public FinalizationRequest finalizationRequest(InputStream input) {
        try {
            byte[] bytes=input.readNBytes(BODY_LIMIT+1);if(bytes.length>BODY_LIMIT)throw InputDraftException.large();
            JsonNode node=mapper.readTree(bytes);requestKeys(node,Set.of("requestId","baseRevision","title","description"));depth(node,1);
            if(!node.path("requestId").isTextual()||!node.path("requestId").asText().matches(UUID_PATTERN))throw InputDraftException.invalid("/requestId");
            JsonNode revision=node.path("baseRevision");
            if(!revision.isIntegralNumber()||!revision.canConvertToLong()||revision.longValue()<0||revision.longValue()>9007199254740991L)throw InputDraftException.invalid("/baseRevision");
            if(!node.path("title").isTextual())throw InputDraftException.invalid("/title");
            String title=node.path("title").asText().trim();
            if(title.isEmpty()||title.length()>200)throw InputDraftException.invalid("/title");
            JsonNode description=node.path("description");String text=null;
            if(!description.isMissingNode()&&!description.isNull()) {
                if(!description.isTextual()||description.asText().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65535)throw InputDraftException.invalid("/description");
                text=description.asText();
            }
            return new FinalizationRequest(node.path("requestId").asText(),revision.longValue(),title,text);
        } catch(InputDraftException ex){throw ex;}
        catch(Exception ex){throw InputDraftException.json();}
    }
    public record ChangeRequest(String requestId,long baseRevision,List<JsonNode> operations) {}
    static boolean restores(ChangeRequest request) { return request.operations().stream().anyMatch(op -> op.path("type").asText().startsWith("RESTORE_")); }
    static int version(ChangeRequest request) { return restores(request) ? 3 : 2; }
    ChangeRequest changeRequest(InputStream input, int version) {
        ChangeRequest request = changeRequest(input);
        if (version != version(request)) throw InputDraftException.internal();
        return request;
    }
    private static final String UUID_PATTERN="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String KEY_PATTERN="[A-Za-z][A-Za-z0-9_-]{0,63}";
    public ChangeRequest changeRequest(InputStream input) {
        try {
            byte[] bytes=input.readNBytes(BODY_LIMIT+1);
            if(bytes.length>BODY_LIMIT)throw InputDraftException.large();
            JsonNode node=mapper.readTree(bytes);
            requestKeys(node,Set.of("requestId","baseRevision","operations"));
            depth(node,1);
            if(node.size()!=3||!node.path("requestId").isTextual()||!node.path("requestId").asText().matches(UUID_PATTERN)
                ||!node.path("baseRevision").isIntegralNumber()||!node.path("baseRevision").canConvertToLong()
                ||node.path("baseRevision").longValue()<0||node.path("baseRevision").longValue()>9007199254740991L
                ||!node.path("operations").isArray()||node.path("operations").isEmpty()||node.path("operations").size()>500)throw InputDraftException.invalid();
            Set<String> commands=new HashSet<>(),deletedRows=new HashSet<>(),deletedColumns=new HashSet<>(),restoredRows=new HashSet<>(),restoredColumns=new HashSet<>();
            Map<String,String> addedRows=new HashMap<>(),addedColumns=new HashMap<>();
            List<JsonNode> operations=new ArrayList<>();int adds=0;
            for(JsonNode op:node.path("operations")) {
                String type=op.path("type").asText();
                String column=op.has("fieldKey")?"fieldKey":"clientColumnKey";
                Set<String> fields=switch(type) {
                    case "ADD_ROW"->Set.of("type","commandId","tableId","clientRowKey","values");
                    case "SET"->op.has("controlId")?Set.of("type","commandId","tableId","controlId","value"):op.has("headerKey")?Set.of("type","commandId","tableId",column,"headerKey","value"):Set.of("type","commandId","tableId","rowRef",column,"value");
                    case "CLEAR"->Set.of("type","commandId","tableId","rowRef",column);
                    case "DELETE_ROW"->Set.of("type","commandId","tableId","rowRef");
                    case "RESTORE_ROW"->Set.of("type","commandId","tableId","rowId");
                    case "RESTORE_COLUMN"->Set.of("type","commandId","tableId","fieldKey");
                    case "ADD_COLUMN"->Set.of("type","commandId","tableId","clientColumnKey","headerValues");
                    case "DELETE_COLUMN"->Set.of("type","commandId","tableId",column);
                    default->throw InputDraftException.invalid();
                };
                requestKeys(op,fields);
                if(op.size()!=fields.size()||!key(op.path("commandId"))||!commands.add(op.path("commandId").asText())
                    ||!op.path("tableId").isTextual()||op.path("tableId").asText().isBlank())throw InputDraftException.invalid();
                String table=op.path("tableId").asText();
                if(type.equals("RESTORE_ROW")) {
                    String id=op.path("rowId").asText();
                    if(!op.path("rowId").isTextual()||!id.matches(UUID_PATTERN)||deletedRows.contains(table+"/id:"+id)||!restoredRows.add(table+"/id:"+id))throw InputDraftException.invalid();
                } else if(type.equals("RESTORE_COLUMN")) {
                    String id=op.path("fieldKey").asText();
                    if(!key(op.path("fieldKey"))||deletedColumns.contains(table+"/id:"+id)||!restoredColumns.add(table+"/id:"+id))throw InputDraftException.invalid();
                } else if(op.has("controlId")) {
                    if(!op.path("controlId").isTextual()||op.path("controlId").asText().isBlank())throw InputDraftException.invalid();
                    scalar(op.path("value"));
                } else if(type.equals("ADD_ROW")) {
                    if(++adds>100||!key(op.path("clientRowKey"))||!op.path("values").isObject()
                        ||addedRows.putIfAbsent(op.path("clientRowKey").asText(),table)!=null)throw InputDraftException.invalid();
                    var values=op.path("values").fields();
                    while(values.hasNext()){var entry=values.next();if(!entry.getKey().matches(KEY_PATTERN))throw InputDraftException.invalid();scalar(entry.getValue());
                        if(deletedColumns.contains(table+"/id:"+entry.getKey()))throw InputDraftException.invalid();}
                } else if(type.equals("ADD_COLUMN")) {
                    if(!key(op.path("clientColumnKey"))||addedColumns.putIfAbsent(op.path("clientColumnKey").asText(),table)!=null)throw InputDraftException.invalid();
                    requestKeys(op.path("headerValues"),Set.of("year","month"));
                    for(JsonNode value:op.path("headerValues"))if(!value.isIntegralNumber())throw InputDraftException.invalid();
                    if(op.path("headerValues").has("month")&&(op.path("headerValues").path("month").bigIntegerValue().compareTo(java.math.BigInteger.ONE)<0
                        ||op.path("headerValues").path("month").bigIntegerValue().compareTo(java.math.BigInteger.valueOf(12))>0))throw InputDraftException.invalid();
                } else {
                    if(!type.equals("DELETE_COLUMN")&&!op.has("headerKey")) {
                        JsonNode ref=op.path("rowRef");requestKeys(ref,Set.of("rowId","clientRowKey"));
                        if(ref.size()!=1)throw InputDraftException.invalid();
                        String row;
                        if(ref.has("rowId")) {
                            if(!ref.path("rowId").isTextual()||!ref.path("rowId").asText().matches(UUID_PATTERN))throw InputDraftException.invalid();
                            row="id:"+ref.path("rowId").asText();
                        } else {
                            if(!key(ref.path("clientRowKey"))||!table.equals(addedRows.get(ref.path("clientRowKey").asText())))throw InputDraftException.invalid();
                            row="key:"+ref.path("clientRowKey").asText();
                        }
                        if(deletedRows.contains(table+"/"+row))throw InputDraftException.invalid();
                        if(type.equals("DELETE_ROW")){if(restoredRows.contains(table+"/"+row))throw InputDraftException.invalid();deletedRows.add(table+"/"+row);}
                    }
                    if(!type.equals("DELETE_ROW")) {
                        if(!key(op.path(column)))throw InputDraftException.invalid();
                        String reference=(column.equals("fieldKey")?"id:":"key:")+op.path(column).asText();
                        if(column.equals("clientColumnKey")&&!table.equals(addedColumns.get(op.path(column).asText())))throw InputDraftException.invalid();
                        if(deletedColumns.contains(table+"/"+reference))throw InputDraftException.invalid();
                        if(type.equals("DELETE_COLUMN")){if(restoredColumns.contains(table+"/"+reference))throw InputDraftException.invalid();deletedColumns.add(table+"/"+reference);}
                        if(type.equals("SET")) {
                            scalar(op.path("value"));
                            if(op.has("headerKey")&&(!op.path("headerKey").isTextual()||!op.path("headerKey").asText().equals("year")||!op.path("value").isIntegralNumber()))throw InputDraftException.invalid();
                        }
                    }
                }
                operations.add(op.deepCopy());
            }
            ChangeRequest request=new ChangeRequest(node.path("requestId").asText(),node.path("baseRevision").longValue(),List.copyOf(operations));
            changeBytes(request);
            return request;
        } catch(IOException ex){throw InputDraftException.json();}
    }
    private static void requestKeys(JsonNode node,Set<String> allowed) {
        if(node==null||!node.isObject())throw InputDraftException.invalid();
        node.fieldNames().forEachRemaining(key->{if(!allowed.contains(key))throw InputDraftException.invalid();});
    }
    private static boolean key(JsonNode node){return node.isTextual()&&node.asText().matches(KEY_PATTERN);}
    private static void depth(JsonNode node,int level) {
        if(level>8)throw InputDraftException.invalid();
        if(node.isContainerNode())node.forEach(child->depth(child,level+1));
    }
    private static void scalar(JsonNode node) {
        if(node.isTextual()){if(node.textValue().length()>4096)throw InputDraftException.invalid();return;}
        if(!node.isNumber())throw InputDraftException.invalid();
        // 평문 10진 직렬화가 body 상한보다 커지는 지수 입력은 저장 전에 거부한다.
        BigDecimal number=node.decimalValue();
        long length=(long)number.precision()+Math.abs((long)number.scale())+2;
        if(length>BODY_LIMIT)throw InputDraftException.large();
    }
    static Object scalarValue(JsonNode value) {return value==null||value.isNull()?null:value.isNumber()?value.decimalValue():value.isBoolean()?value.booleanValue():value.asText();}
    public boolean sameRequest(String persisted,ChangeRequest incoming) {
        return semantic(payload(persisted)).equals(semantic(tree(incoming)));
    }
    public boolean sameValue(JsonNode left,JsonNode right) {return semantic(left).equals(semantic(right));}
    private JsonNode semantic(JsonNode node) {
        if(node.isNumber())return com.fasterxml.jackson.databind.node.DecimalNode.valueOf(node.decimalValue().stripTrailingZeros());
        if(node.isObject()){ObjectNode result=mapper.createObjectNode();node.fields().forEachRemaining(e->result.set(e.getKey(),semantic(e.getValue())));return result;}
        if(node.isArray()){ArrayNode result=mapper.createArrayNode();node.forEach(e->result.add(semantic(e)));return result;}
        return node;
    }
    public JsonNode payload(String text) {
        if(text==null||text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>BODY_LIMIT)throw InputDraftException.internal();
        try {JsonNode node=mapper.readTree(text);if(node==null)throw InputDraftException.internal();return node;}
        catch(IOException ex){throw InputDraftException.internal();}
    }
    JsonNode uploadMetadata(String text) {
        if(text==null)throw InputDraftException.invalid();
        if(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>BODY_LIMIT)throw InputDraftException.large();
        try {JsonNode node=mapper.readTree(text);if(node==null||!node.isObject())throw InputDraftException.invalid();return node;}
        catch(IOException ex){throw InputDraftException.json();}
    }
    public JsonNode definition(String text) {
        if(text==null) throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        if(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>BODY_LIMIT) throw InputDraftException.internal();
        try { return mapper.readTree(text); } catch(IOException ex) { throw InputDraftException.conflict("POLICY_INCOMPLETE",null); }
    }
    public byte[] bytes(Object value) {
        try { return mapper.writeValueAsBytes(value); } catch(IOException ex) { throw InputDraftException.internal(); }
    }
    public byte[] changeBytes(ChangeRequest request) {
        try {
            byte[] bytes=changeWriter.writeValueAsBytes(request);
            if(bytes.length>BODY_LIMIT)throw InputDraftException.large();
            return bytes;
        } catch(IOException ex){throw InputDraftException.internal();}
    }
    public JsonNode delta(String text) {
        if(text==null||text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>DELTA_LIMIT)throw InputDraftException.internal();
        try {JsonNode value=mapper.readTree(text);saved(value,1);return value;}
        catch(IOException|ArithmeticException ex){throw InputDraftException.internal();}
    }
    public String deltaText(JsonNode value) {
        saved(value,1);
        try {
            byte[] bytes=changeWriter.writeValueAsBytes(value);
            if(bytes.length>DELTA_LIMIT)throw InputDraftException.internal();
            return new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
        }catch(IOException ex){throw InputDraftException.internal();}
    }
    private static void saved(JsonNode value,int depth) {
        if(value==null||depth>16)throw InputDraftException.internal();
        if(value.isTextual()&&value.textValue().length()>4096)throw InputDraftException.internal();
        if(value.isNumber()&&(long)value.decimalValue().precision()+Math.abs((long)value.decimalValue().scale())+2>DELTA_LIMIT)throw InputDraftException.internal();
        if(value.isObject())value.fieldNames().forEachRemaining(key->{if(key.length()>4096)throw InputDraftException.internal();});
        if(value.isContainerNode())value.forEach(child->saved(child,depth+1));
    }
    public JsonNode tree(Object value) { return mapper.valueToTree(value); }
    public static void keys(JsonNode node, Set<String> allowed) {
        if(!node.isObject()) throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        node.fieldNames().forEachRemaining(key->{ if(!allowed.contains(key)) throw InputDraftException.conflict("POLICY_INCOMPLETE",null); });
    }
}
