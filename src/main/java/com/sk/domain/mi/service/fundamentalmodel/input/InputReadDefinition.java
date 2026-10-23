package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 고정 발행 묶음의 내부 조회 정의. 누락된 업무 정책을 추정하지 않는다. */
public record InputReadDefinition(List<Integer> columns,List<Integer> headerRows,List<Range> bodyRanges,
        List<Integer> identityColumns,JsonNode fields,JsonNode capabilities,JsonNode inputGuideRules,
        JsonNode controls,JsonNode displayUpdateRules,JsonNode columnCreation,boolean navigationVisible) {
    public record Range(int startRow,int endRow) {}
    public static InputReadDefinition parse(JsonNode root) {
        if(root==null || !root.isObject()) throw incomplete();
        InputDraftJson.keys(root,Set.of("schemaVersion","columns","headerRows","bodyRanges","identityColumns",
            "fields","capabilities","inputGuideRules","controls","displayUpdateRules","columnCreation","uploadValidation","navigationVisible"));
        if(root.has("navigationVisible")&&!root.path("navigationVisible").isBoolean()) throw incomplete();
        if(!root.path("schemaVersion").isIntegralNumber()) throw incomplete();
        if(!root.path("schemaVersion").canConvertToInt() || root.path("schemaVersion").intValue()!=1) throw InputDraftException.conflict("POLICY_UNSUPPORTED",null);
        List<Integer> columns=ints(root.path("columns"),16384), headers=ints(root.path("headerRows"),1048576),
            identity=ints(root.path("identityColumns"),16384);
        JsonNode ranges=root.path("bodyRanges"); if(!ranges.isArray()) throw incomplete();
        List<Range> body=new ArrayList<>(); int end=0;
        for(JsonNode range:ranges) {
            InputDraftJson.keys(range,Set.of("startRow","endRow"));
            if(!range.path("startRow").isIntegralNumber()||!range.path("endRow").isIntegralNumber()
                ||!range.path("startRow").canConvertToInt()||!range.path("endRow").canConvertToInt()) throw incomplete();
            int start=range.path("startRow").intValue(),last=range.path("endRow").intValue();
            if(start<=end||last<start||last>1048576||headers.stream().anyMatch(row->row>=start&&row<=last)) throw incomplete();
            body.add(new Range(start,last)); end=last;
        }
        if(!columns.containsAll(identity)||(!body.isEmpty()&&identity.isEmpty())) throw incomplete();
        for(String name:List.of("fields","inputGuideRules","controls","displayUpdateRules")) if(!root.path(name).isArray()) throw incomplete();
        JsonNode caps=root.path("capabilities").deepCopy(); InputDraftJson.keys(caps,Set.of("SET","CLEAR","ADD_ROW","DELETE_ROW","ADD_COLUMN","DELETE_COLUMN"));
        for(String name:List.of("ADD_COLUMN","DELETE_COLUMN")) {
            if(!caps.has(name))((com.fasterxml.jackson.databind.node.ObjectNode)caps).put(name,"INCOMPLETE");
            status(caps.path(name));
        }
        JsonNode fields=root.path("fields").deepCopy();
        for(String name:List.of("SET","CLEAR","ADD_ROW","DELETE_ROW")) status(caps.path(name));
        Set<String> fieldKeys=new HashSet<>(); Set<Integer> fieldColumns=new HashSet<>();
        for(JsonNode field:fields) {
            InputDraftJson.keys(field,Set.of("fieldKey","columnNumber","dataType","policyStatus","create","setConstraints"));
            if(!field.path("columnNumber").isIntegralNumber()||!field.path("columnNumber").canConvertToInt()) throw incomplete();
            String key=field.path("fieldKey").asText(); int col=field.path("columnNumber").asInt();
            if(!key.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")||!fieldKeys.add(key)||!columns.contains(col)||!fieldColumns.add(col)
                ||!Set.of("INTEGER","DECIMAL","TEXT","DECIMAL_OR_TEXT").contains(field.path("dataType").asText())) throw incomplete();
            JsonNode policy=field.path("policyStatus"); InputDraftJson.keys(policy,Set.of("CREATE","SET","CLEAR","DELETE_COLUMN","SET_HEADER"));
            if(!policy.has("SET_HEADER"))((com.fasterxml.jackson.databind.node.ObjectNode)policy).put("SET_HEADER","INCOMPLETE");
            status(policy.path("SET_HEADER"));
            if(!policy.has("DELETE_COLUMN"))((com.fasterxml.jackson.databind.node.ObjectNode)policy).put("DELETE_COLUMN","INCOMPLETE");
            status(policy.path("DELETE_COLUMN"));
            for(String name:List.of("CREATE","SET","CLEAR")) status(policy.path(name));
            if("READY".equals(policy.path("CREATE").asText()) && (!field.path("create").isObject()
                ||!field.path("create").path("required").isBoolean()||!field.path("create").path("constraints").isObject()
                ||!Set.of("EMPTY","VALUE").contains(field.path("create").path("initial").path("kind").asText()))) throw incomplete();
            if("READY".equals(policy.path("SET").asText())&&!field.path("setConstraints").isObject()) throw incomplete();
            if(!"READY".equals(policy.path("CREATE").asText())&&field.has("create")) throw incomplete();
            if(!"READY".equals(policy.path("SET").asText())&&field.has("setConstraints")) throw incomplete();
            if("VALUE".equals(field.path("create").path("initial").path("kind").asText())
                &&(!field.path("create").path("initial").hasNonNull("value"))) throw incomplete();
        }
        JsonNode creation=root.get("columnCreation");
        if("READY".equals(caps.path("ADD_COLUMN").asText())) {
            if(creation==null)throw incomplete();
            InputDraftJson.keys(creation,Set.of("headerValues","body"));
            if(creation.size()!=2||!creation.path("headerValues").isObject())throw incomplete();
            InputDraftJson.keys(creation.path("headerValues"),Set.of("year","month"));
            for(JsonNode header:creation.path("headerValues"))creation(header);
            creation(creation.path("body"));
        } else if(creation!=null)throw incomplete();
        Set<String> controlIds=new HashSet<>();
        for(JsonNode control:root.path("controls")) {
            InputDraftJson.keys(control,Set.of("id","type","label","position","sourceTableId","binding","options","editable"));
            if(!control.isObject()||!control.path("id").isTextual()||!control.path("binding").path("rowNumber").canConvertToInt()
                ||!control.path("binding").path("columnIndex").canConvertToInt()||!control.path("options").isArray()
                ||!control.path("editable").isBoolean()||!control.path("binding").path("rowNumber").isIntegralNumber()
                ||!control.path("binding").path("columnIndex").isIntegralNumber()||!controlIds.add(control.path("id").asText())) throw incomplete();
            int row=control.path("binding").path("rowNumber").asInt(),col=control.path("binding").path("columnIndex").asInt();
            if(row<1||row>1048576||col<0||col>=16384) throw incomplete();
            InputDraftJson.keys(control.path("binding"),Set.of("rowNumber","columnIndex"));
            if(control.path("id").asText().isBlank()||!"select".equals(control.path("type").asText())
                ||!control.path("label").isTextual()||!"beforeGrid".equals(control.path("position").asText())
                ||!control.path("sourceTableId").isTextual()||control.path("sourceTableId").asText().isBlank()) throw incomplete();
            Set<JsonNode> options=new HashSet<>();
            for(JsonNode option:control.path("options")) {
                InputDraftJson.keys(option,Set.of("value","label"));
                if(!(option.path("value").isTextual()||option.path("value").isNumber())
                    ||!option.path("label").isTextual()||!options.add(option.get("value"))) throw incomplete();
            }
        }
        return new InputReadDefinition(List.copyOf(columns),List.copyOf(headers),List.copyOf(body),List.copyOf(identity),
            fields,caps,root.get("inputGuideRules"),root.get("controls"),root.get("displayUpdateRules"),creation,root.path("navigationVisible").asBoolean(true));
    }
    private static void creation(JsonNode value) {
        InputDraftJson.keys(value,Set.of("dataType","create"));
        if(value.size()!=2||!Set.of("INTEGER","DECIMAL","TEXT").contains(value.path("dataType").asText()))throw incomplete();
        JsonNode create=value.path("create");InputDraftJson.keys(create,Set.of("required","initial","constraints"));
        if(create.size()!=3||!create.path("required").isBoolean()||!create.path("constraints").isObject())throw incomplete();
        JsonNode initial=create.path("initial");InputDraftJson.keys(initial,Set.of("kind","value"));
        String kind=initial.path("kind").asText();
        if(kind.equals("EMPTY")){if(initial.size()!=1)throw incomplete();}
        else if(kind.equals("VALUE")) {
            JsonNode scalar=initial.path("value");
            if(initial.size()!=2||!(value.path("dataType").asText().equals("TEXT")?scalar.isTextual():scalar.isNumber()))throw incomplete();
            if(value.path("dataType").asText().equals("INTEGER"))try{scalar.decimalValue().toBigIntegerExact();}catch(ArithmeticException ex){throw incomplete();}
        } else throw incomplete();
    }
    private static List<Integer> ints(JsonNode node,int maximum) {
        if(!node.isArray()) throw incomplete(); List<Integer> values=new ArrayList<>(); int previous=0;
        for(JsonNode val:node) { if(!val.isIntegralNumber()||!val.canConvertToInt()||val.intValue()<=previous||val.intValue()>maximum) throw incomplete(); previous=val.intValue();values.add(previous); }
        return values;
    }
    private static void status(JsonNode node) { if(!Set.of("READY","INCOMPLETE","NOT_ALLOWED","UNSUPPORTED").contains(node.asText())) throw incomplete(); }
    private static InputDraftException incomplete() { return InputDraftException.conflict("POLICY_INCOMPLETE",null); }
    public int totalRows() { return bodyRanges.stream().mapToInt(r->r.endRow-r.startRow+1).sum(); }
    public boolean body(int row) { return bodyRanges.stream().anyMatch(r->row>=r.startRow&&row<=r.endRow); }
    public List<Integer> page(Integer first,int limit) {
        if(first!=null&&!body(first)) throw InputDraftException.invalid();
        List<Integer> result=new ArrayList<>();
        for(Range range:bodyRanges) for(int row=Math.max(range.startRow,first==null?range.startRow:first);row<=range.endRow&&result.size()<limit;row++) result.add(row);
        return result;
    }
}
