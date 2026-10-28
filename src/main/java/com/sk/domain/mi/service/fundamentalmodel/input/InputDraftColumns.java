package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.TableRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Draft 안의 원본 열 identity와 현재 위치. 삭제 identity도 계속 보관한다. */
final class InputDraftColumns {
    record Column(String field,Integer source,Integer position,String client) {}
    private final Map<Long,Map<String,Column>> tables=new LinkedHashMap<>();
    private final Map<Long,Map<Integer,String>> positions=new HashMap<>();

    InputDraftColumns(List<TableRef> catalog,Map<Long,InputReadDefinition> definitions) {
        for(TableRef table:catalog) {
            Map<Integer,String> fields=new HashMap<>();
            definitions.get(table.id()).fields().forEach(field->fields.put(field.path("columnNumber").asInt(),field.path("fieldKey").asText()));
            Map<String,Column> columns=new LinkedHashMap<>();
            for(int n=table.startColumn();n<=table.endColumn();n++)columns.put("s"+n,new Column(fields.get(n),n,n,null));
            tables.put(table.id(),columns);index(table.id());
        }
    }
    InputDraftColumns(InputDraftColumns source) {source.tables.forEach((id,columns)->{tables.put(id,new LinkedHashMap<>(columns));index(id);});}
    void reset(TableRef table,InputReadDefinition definition) {
        InputDraftColumns original=new InputDraftColumns(List.of(table),Map.of(table.id(),definition));
        tables.put(table.id(),new LinkedHashMap<>(original.tables.get(table.id())));index(table.id());
    }
    Column field(long table,String key) {
        return tables.get(table).values().stream().filter(c->key.equals(c.field)).findFirst().orElse(null);
    }
    List<Column> all(long table) {return List.copyOf(tables.get(table).values());}
    private void index(long table) {
        Map<Integer,String> current=new HashMap<>();tables.get(table).forEach((key,column)->{if(column.position!=null)require(current.putIfAbsent(column.position,key)==null);});positions.put(table,current);
    }
    boolean active(long table,int position) {return positions.get(table).containsKey(position);}
    Integer source(long table,int current) {
        String key=positions.get(table).get(current);return key==null?null:tables.get(table).get(key).source;
    }
    Integer moved(long table,int previous,InputDraftColumns before) {
        String key=before.positions.get(table).get(previous);return key==null?null:tables.get(table).get(key).position;
    }
    Map<String,Column> apply(TableRef table,JsonNode changes,Set<String> touched) {return apply(table,changes,touched,Set.of());}
    Map<String,Column> apply(TableRef table,JsonNode changes,Set<String> touched,Set<String> restored) {
        Map<String,Column> added=new LinkedHashMap<>(),columns=tables.get(table.id());
        for(JsonNode change:changes)if(table.key().equals(change.path("tableId").asText())) {
            exact(change,"tableId","fieldKey","sourceColumnNumber","columnNumber","clientColumnKey");
            String field=nullableText(change.get("fieldKey")),client=nullableText(change.get("clientColumnKey"));
            Integer source=nullableColumn(change.get("sourceColumnNumber")),position=nullableColumn(change.get("columnNumber"));
            require(source!=null||field!=null);if(field!=null)require(field.matches("[A-Za-z][A-Za-z0-9_-]{0,63}"));
            String identity=source==null?"f"+field:"s"+source;
            require(touched.add(table.id()+"/"+identity));Column previous=columns.get(identity);
            if(previous!=null) {
                require((previous.position!=null||previous.source!=null&&position!=null&&restored.contains(table.key()+"/"+field))&&java.util.Objects.equals(field,previous.field)&&client==null);
            } else {
                require(source==null&&client!=null&&client.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")&&field(table.id(),field)==null);
            }
            Column current=new Column(field,source,position,previous==null?client:previous.client);
            columns.put(identity,current);if(previous==null)require(added.putIfAbsent(client,current)==null);
        }
        index(table.id());return added;
    }
    void validate(TableRef table,InputReadDefinition definition) {
        Set<Integer> positions=new HashSet<>();Set<String> fields=new HashSet<>();
        for(Column column:tables.get(table.id()).values()) {
            if(column.field!=null)require(fields.add(column.field));
            if(column.position!=null)require(column.position>=table.startColumn()&&column.position<=table.endColumn()&&positions.add(column.position));
        }
        // 표시에서 빠진 꼬리 슬롯도 더는 원본 열로 복구하지 않는다.
        require(positions.containsAll(definition.columns()));
        Set<String> visible=new HashSet<>();
        for(JsonNode field:definition.fields()) {
            Column column=field(table.id(),field.path("fieldKey").asText());
            require(column!=null&&column.position!=null&&column.position==field.path("columnNumber").asInt());visible.add(column.field);
        }
        for(Column column:tables.get(table.id()).values())if(column.field!=null&&column.position!=null)require(visible.contains(column.field));
    }
    List<Map<String,Object>> commands(List<JsonNode> commands,InputDraftColumns before,Map<Long,TableRef> tables,
            Map<Long,Map<String,Column>> additions) {
        Map<Long,Set<String>> live=new HashMap<>();
        before.tables.forEach((id,cols)->{Set<String> keys=new HashSet<>();cols.values().stream().filter(c->c.field!=null&&c.position!=null).forEach(c->keys.add(c.field));live.put(id,keys);});
        Map<String,String> clients=new HashMap<>();Set<String> used=new HashSet<>();List<Map<String,Object>> mappings=new ArrayList<>();
        for(JsonNode command:commands) {
            TableRef table=tables.values().stream().filter(t->t.key().equals(command.path("tableId").asText())).findFirst().orElseThrow(InputDraftException::internal);
            String type=command.path("type").asText();
            if(type.equals("RESTORE_COLUMN")) {
                String field=command.path("fieldKey").asText();Column previous=before.field(table.id(),field),current=field(table.id(),field);
                require(previous!=null&&previous.source!=null&&previous.position==null&&current!=null&&current.position!=null);live.get(table.id()).add(field);
            } else if(type.equals("ADD_COLUMN")) {
                String client=command.path("clientColumnKey").asText();Column added=additions.getOrDefault(table.id(),Map.of()).get(client);
                require(added!=null&&used.add(client));clients.put(client,added.field);live.get(table.id()).add(added.field);
                mappings.add(Map.of("commandId",command.path("commandId").asText(),"clientColumnKey",client,"tableId",table.key(),"fieldKey",added.field,"columnState",added.position==null?"DELETED":"ACTIVE"));
            } else if(!command.has("controlId")&&(type.equals("SET")||type.equals("CLEAR")||type.equals("DELETE_COLUMN"))) {
                String field=resolve(command,clients);require(field!=null&&live.get(table.id()).contains(field));
                if(type.equals("DELETE_COLUMN"))live.get(table.id()).remove(field);
            } else if(type.equals("ADD_ROW"))command.path("values").fieldNames().forEachRemaining(field->require(live.get(table.id()).contains(field)));
        }
        Set<String> allAdded=new HashSet<>();additions.values().forEach(cols->cols.keySet().forEach(client->require(allAdded.add(client))));require(used.equals(allAdded));
        this.tables.forEach((id,cols)->cols.values().stream().filter(c->c.field!=null).forEach(c->require(live.get(id).contains(c.field)==(c.position!=null))));
        return List.copyOf(mappings);
    }
    private static String resolve(JsonNode command,Map<String,String> clients) {return command.has("fieldKey")?command.path("fieldKey").asText():clients.get(command.path("clientColumnKey").asText());}
    private static Integer nullableColumn(JsonNode node) {require(node!=null);if(node.isNull())return null;require(node.isIntegralNumber()&&node.canConvertToInt()&&node.intValue()>=1&&node.intValue()<=16384);return node.intValue();}
    private static String nullableText(JsonNode node) {require(node!=null&&(node.isNull()||node.isTextual()));return node.isNull()?null:node.textValue();}
    private static void exact(JsonNode node,String... keys) {require(node.isObject()&&node.size()==keys.length);Set<String> allowed=Set.of(keys);node.fieldNames().forEachRemaining(key->require(allowed.contains(key)));}
    private static void require(boolean valid) {if(!valid)throw InputDraftException.internal();}
}
