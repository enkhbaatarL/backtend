package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 고정 원본 + 성공 명령을 같은 실제 파일 경로로 재생하고, 저장 후 재읽은 값만 v2에 보낸다. */
final class InputWorkbookEditor {
    private final InputWorkbookReader reader;
    private final InputTempFiles scratch;
    private final InputDraftJson json;
    private final InputWorkbookXml.Checkpoint checkpoint;

    InputWorkbookEditor(InputWorkbookReader reader, InputTempFiles scratch, InputDraftJson json, InputWorkbookXml.Checkpoint checkpoint) {
        this.reader = reader; this.scratch = scratch; this.json = json; this.checkpoint = checkpoint;
    }

    static boolean supports(TableRef table, InputReadDefinition definition, String action) {
        return InputWorkbookPolicy.supports(table, definition, action);
    }

    static void validate(ModelInputDraftService.Source source, InputDraftHistory history,
            InputDraftJson.ChangeRequest request, long revision) {
        Map<Long, Map<Integer, String>> bindings = new HashMap<>();
        source.catalog().tables().forEach(table -> bindings.put(table.id(), history.bindings(table.id())));
        Set<String> addedRows = bindings.values().stream().flatMap(rows -> rows.values().stream()).filter(id -> history.origin(id).equals("ADDED"))
            .collect(java.util.stream.Collectors.toSet());
        Set<String> addedFields = new java.util.HashSet<>();
        source.catalog().tables().forEach(table -> source.definitions().get(table.id()).fields().forEach(field -> {
            int column = field.path("columnNumber").asInt();
            if (history.sourceColumn(table.id(), column) == null) addedFields.add(table.key() + "/" + field.path("fieldKey").asText());
        }));
        validate(source, bindings, addedRows, addedFields, request, revision);
    }

    static void validate(ModelInputDraftService.Source source, Map<Long, Map<Integer, String>> bindings,
            Set<String> addedRows, Set<String> addedFields,
            InputDraftJson.ChangeRequest request, long revision) {
        InputWorkbookPolicy.validate(source, bindings, addedRows, addedFields, request, revision);
    }

    static void validateHistory(ModelInputDraftService.Source source, List<Revision> revisions, InputDraftJson json) {
        for (Revision revision : revisions) {
            if (revision.schema() != 2 || revision.number() > 0 && revision.operationSchema() != 2) throw InputDraftException.internal();
            if (revision.number() == 0 || revision.type().equals("UPLOAD_FILE")) continue;
            JsonNode delta = json.delta(revision.delta());
            for (JsonNode command : delta.path("commands")) {
                TableRef table = source.catalog().tables().stream().filter(t -> t.key().equals(command.path("tableId").asText())).findFirst().orElseThrow(InputDraftException::internal);
                if (!supports(table, source.definitions().get(table.id()), command.path("type").asText())) throw InputDraftException.conflict("POLICY_UNSUPPORTED", revision.number());
            }
        }
    }

    JsonNode execute(InputDraftEditingBasis basis, String company, InputDraftJson.ChangeRequest request) {
        return InputDraftDiagnostics.withOperation(basis.draft().activeOperation(), () ->
            InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.CHANGE_EXECUTE, null, () -> executeCurrent(basis, company, request)));
    }
    private JsonNode executeCurrent(InputDraftEditingBasis basis, String company, InputDraftJson.ChangeRequest request) {
        var current = InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.HISTORY_RESTORE, null, () -> basis.history(basis.draft().revision(), json));
        var source = currentSource(basis, current); validate(source, current, request, basis.draft().revision());
        ObjectNode delta = delta(basis, request);
        Set<String> roles = request.operations().stream().map(c -> table(current, c.path("tableId").asText()).role()).collect(java.util.stream.Collectors.toSet());
        for (String role : roles) {
            FileRef file = basis.source().files().stream().filter(f -> f.role().equals(role)).findFirst().orElseThrow(() -> InputDraftException.conflict("FILES_INCOMPLETE", basis.draft().revision()));
            JsonNode result = reader.withLocal(file, company, original -> {
                List<Path> owned = new ArrayList<>();
                try {
                    Path before = InputDraftDiagnostics.timedChecked(InputDraftDiagnostics.Stage.CHANGE_REPLAY, role,
                        () -> replay(basis, file, original, owned));
                    Path saved = create(owned);
                    return InputDraftDiagnostics.timedChecked(InputDraftDiagnostics.Stage.CHANGE_TRANSFORM, role,
                        () -> new InputWorkbookCommands(json, reader, checkpoint).transform(before, saved, current, request, role, null, original));
                } catch (InputDraftException | InputDraftPreserveChange.Failure ex) { throw ex; }
                catch (Exception ex) { throw new InputDraftPreserveChange.Failure("PRESERVATION_FAILED"); }
                finally { cleanup(owned); }
            });
            for (String key : List.of("tables", "rows", "columns", "cells")) for (JsonNode item : result.path(key)) delta.withArray(key).add(item);
        }
        // 같은 v2 검증기를 통과하기 전에는 worker가 성공 transaction을 만들지 않는다.
        InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.CHANGE_APPLY, null, () ->
            current.apply(delta, new String(json.changeBytes(request), StandardCharsets.UTF_8), java.util.UUID.randomUUID().toString(), request.baseRevision() + 1));
        return delta;
    }

    void finalFile(InputDraftEditingBasis basis, FileRef file, Path original, Path target) throws Exception {
        List<Path> owned = new ArrayList<>();
        try {
            Path current = replay(basis, file, original, owned); checkpoint.check();
            Files.copy(current, target, StandardCopyOption.REPLACE_EXISTING);
            if (!InputDraftFinalizationFiles.digest(current, checkpoint::check).equals(InputDraftFinalizationFiles.digest(target, checkpoint::check)))
                throw new InputDraftPreserveChange.Failure("PRESERVATION_FAILED");
        } finally { cleanup(owned); }
    }

    private Path replay(InputDraftEditingBasis basis, FileRef file, Path original, List<Path> owned) throws Exception {
        try { InputDraftFinalizationFiles.validatePackage(original, checkpoint::check); }
        catch (InputDraftPreserveChange.Failure ex) { throw ex; }
        catch (Exception ex) { throw new InputDraftPreserveChange.Failure("SOURCE_CHANGED"); }
        Path current = original;
        for (Revision revision : basis.revisions()) if (revision.number() > file.baseline() && revision.type().equals("CHANGE")) {
            checkpoint.check(); var history = basis.history(revision.number() - 1, json);
            var request = json.changeRequest(new ByteArrayInputStream(revision.request().getBytes(StandardCharsets.UTF_8)));
            if (request.operations().stream().noneMatch(c -> table(history, c.path("tableId").asText()).role().equals(file.role()))) continue;
            JsonNode recorded = json.delta(revision.delta());
            Path saved = create(owned);
            JsonNode actual = new InputWorkbookCommands(json, reader, checkpoint).transform(current, saved, history, request, file.role(), recorded, original);
            for (String kind : List.of("tables", "rows", "columns", "cells")) {
                Map<String, JsonNode> expected = new HashMap<>();
                for (JsonNode item : recorded.path(kind)) if (table(history, item.path("tableId").asText()).role().equals(file.role())) expected.put(key(kind, item), item);
                if (expected.size() != actual.path(kind).size()) throw new InputDraftPreserveChange.Failure("PRESERVATION_FAILED");
                for (JsonNode item : actual.path(kind)) if (!json.sameValue(item, expected.get(key(kind, item)))) throw new InputDraftPreserveChange.Failure("PRESERVATION_FAILED");
            }
            if (!current.equals(original)) Files.delete(current); current = saved;
        }
        return current;
    }

    private Path create(List<Path> owned) throws java.io.IOException { Path path = scratch.create(); owned.add(path); return path; }
    private static void cleanup(List<Path> owned) {
        boolean failed = false; for (Path path : owned) try { Files.deleteIfExists(path); } catch (Exception ex) { failed = true; }
        if (failed) throw new InputDraftPreserveChange.Failure("PERSISTENCE_FAILED");
    }
    private ObjectNode delta(InputDraftEditingBasis basis, InputDraftJson.ChangeRequest request) { return (ObjectNode) json.tree(Map.of("policyTemplateId", basis.source().catalog().templateId(), "commands", request.operations(), "tables", List.of(), "rows", List.of(), "columns", List.of(), "cells", List.of())); }
    private static ModelInputDraftService.Source currentSource(InputDraftEditingBasis basis, InputDraftHistory history) {
        var s = basis.source(); return new ModelInputDraftService.Source(s.caseId(), history.catalog(), s.files(), s.key(), s.version(), s.inputIds(), history.definitions());
    }
    private static TableRef table(InputDraftHistory history, String key) { return history.catalog().tables().stream().filter(t -> t.key().equals(key)).findFirst().orElseThrow(InputDraftException::missing); }
    private static String key(String kind, JsonNode item) {
        String table = item.path("tableId").asText();
        return switch (kind) {
            case "tables" -> table;
            case "rows" -> table + "/" + item.path("rowId").asText();
            case "columns" -> table + "/" + item.path("fieldKey") + "/" + item.path("sourceColumnNumber");
            default -> table + "/" + item.path("rowId") + "/" + item.path("rowNumber") + "/" + item.path("columnNumber");
        };
    }
}
