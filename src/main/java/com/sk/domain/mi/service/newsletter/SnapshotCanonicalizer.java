package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import org.springframework.stereotype.Component;

@Component
public class SnapshotCanonicalizer {

    public JsonNode canonicalize(JsonNode snapshot) {
        if (snapshot == null || snapshot.isNull()) {
            return snapshot;
        }

        JsonNode copied = snapshot.deepCopy();
        if (!(copied instanceof ObjectNode root)) {
            return copied;
        }

        JsonNode sectionsNode = root.get("sections");
        if (!(sectionsNode instanceof ArrayNode sections)) {
            return root;
        }

        for (JsonNode sectionNode : sections) {
            if (sectionNode instanceof ObjectNode sectionObject) {
                canonicalizeSection(sectionObject);
            }
        }
        return root;
    }

    private void canonicalizeSection(ObjectNode section) {
        String sectionCode = resolveSectionCode(section);
        if (sectionCode != null && !sectionCode.isBlank()) {
            if (isBlank(section.get("code"))) {
                section.put("code", sectionCode);
            }
            if (isBlank(section.get("name")) && section.get("title") != null && !section.get("title").isNull()) {
                section.put("name", section.get("title").asText());
            }
        }
        moveSectionRemoved(section);
        section.remove("title");
        if (section.get("fields") == null && section.get("content") instanceof ObjectNode contentObject) {
            section.set("fields", contentObject.deepCopy());
        }
        section.remove("content");

        JsonNode blocksNode = section.get("blocks");
        if (!(blocksNode instanceof ArrayNode blocks)) {
            return;
        }

        for (JsonNode blockNode : blocks) {
            if (blockNode instanceof ObjectNode blockObject) {
                canonicalizeBlock(blockObject);
            }
        }
    }

    private void canonicalizeBlock(ObjectNode block) {
        JsonNode contentNode = block.get("content");
        if (contentNode instanceof ObjectNode contentObject && block.get("fields") == null) {
            block.set("fields", contentObject.deepCopy());
        }

        JsonNode fieldDefsNode = block.get("fieldDefs");
        if ((fieldDefsNode == null || fieldDefsNode.isNull()) && contentNode instanceof ObjectNode contentObject) {
            ArrayNode editableFieldsNode = block.get("editableFields") instanceof ArrayNode editableArray ? editableArray : null;
            ObjectNode generatedFieldDefs = JsonNodeFactory.instance.objectNode();
            Iterator<String> fieldNames = contentObject.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                generatedFieldDefs.set(fieldName, buildFieldDefNode(containsEditableField(editableFieldsNode, fieldName)));
            }
            if (!generatedFieldDefs.isEmpty()) {
                block.set("fieldDefs", generatedFieldDefs);
            }
        }

        block.remove("content");
        block.remove("editableFields");
    }

    private ObjectNode buildFieldDefNode(boolean editable) {
        ObjectNode fieldDef = JsonNodeFactory.instance.objectNode();
        fieldDef.set("editable", BooleanNode.valueOf(editable));
        fieldDef.set("required", BooleanNode.FALSE);
        return fieldDef;
    }

    private boolean containsEditableField(ArrayNode editableFieldsNode, String fieldName) {
        if (editableFieldsNode == null) {
            return false;
        }
        for (JsonNode editableFieldNode : editableFieldsNode) {
            if (editableFieldNode != null && fieldName.equals(editableFieldNode.asText())) {
                return true;
            }
        }
        return false;
    }

    private String resolveSectionCode(ObjectNode section) {
        JsonNode codeNode = section.get("code");
        if (!isBlank(codeNode)) {
            return codeNode.asText();
        }

        JsonNode nameNode = section.get("name");
        if (!isBlank(nameNode)) {
            return nameNode.asText();
        }

        JsonNode idNode = section.get("id");
        if (idNode != null && !idNode.isNull()) {
            String id = idNode.asText();
            if (id.startsWith("sec-") && id.length() > 4) {
                String body = id.substring(4);
                int lastDash = body.lastIndexOf('-');
                return lastDash > 0 ? body.substring(0, lastDash) : body;
            }
        }
        return null;
    }

    private boolean isBlank(JsonNode node) {
        return node == null || node.isNull() || node.asText().isBlank();
    }

    private void moveSectionRemoved(ObjectNode section) {
        if (section.get("removed") != null && !section.get("removed").isNull()) {
            section.remove("state");
            return;
        }

        if (section.get("state") instanceof ObjectNode stateObject) {
            JsonNode removedNode = stateObject.get("removed");
            if (removedNode != null && !removedNode.isNull()) {
                section.set("removed", removedNode.deepCopy());
            }
        }
        section.remove("state");
    }

}
