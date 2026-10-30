package com.sk.domain.mi.service.fundamentalmodel.input;

import static com.sk.domain.mi.service.fundamentalmodel.input.InputDraftFinalizationService.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 기존 master JDBC 경계에 참여한다. A가 같은 masterDataSource/transactionManager로 명시 생성한다. */
final class InputDraftFinalizationRepository {
    record Prepared(String role, String status, Reference ref, Long fileNo) {}
    private final JdbcTemplate sql;
    private final ModelInputDraftRepository common;
    private final InputDraftJson json;
    private final BasisVerifier basis;

    InputDraftFinalizationRepository(DataSource master, ModelInputDraftRepository common,
            InputDraftJson json, BasisVerifier basis) {
        sql = new JdbcTemplate(master);
        this.common = Objects.requireNonNull(common);
        this.json = Objects.requireNonNull(json);
        this.basis = Objects.requireNonNull(basis);
    }

    Operation accept(String id, int company, int user, String actor, String role, Request request) {
        Draft draft = common.lockOwned(id, company, user);
        if (actor == null || actor.isBlank() || actor.length() > 50) throw InputDraftException.missing();
        Operation candidate = new Operation(UUID.randomUUID().toString(), id, request.requestId(), "FINALIZE", role,
                request.baseRevision(), "QUEUED", 2, encode(request), null, common.databaseTime().withNano(0).plusSeconds(600), null, null, actor);
        common.workerOwner(draft, candidate);
        if(!draft.uploaded())common.caseAccess(draft.caseId(), false);
        Operation existing = common.request(id, request.requestId());
        if (existing != null) {
            if (!existing.type().equals("FINALIZE") || !same(readRequest(existing), request)) {
                throw InputDraftException.conflict("IDEMPOTENCY_CONFLICT", draft.revision());
            }
            return existing;
        }
        if (draft.status().equals("FINALIZED")) {
            List<String> ids = sql.query("SELECT prcs_id FROM tmd_mdl_inptdrftprcsl WHERE drft_id=? AND prcs_type_cd='FINALIZE' AND prcs_stat_cd='SUCCEEDED'",
                    (rs, n) -> rs.getString(1), id);
            if (ids.size() != 1) throw InputDraftException.internal();
            Operation succeeded = common.operation(id, ids.get(0), true);
            if (!same(readRequest(succeeded), request)) throw InputDraftException.conflict("DRAFT_NOT_EDITABLE", draft.revision());
            return succeeded;
        }
        if (!draft.status().equals("DRAFT")) throw InputDraftException.conflict("DRAFT_NOT_EDITABLE", draft.revision());
        verify(draft, candidate);
        if (draft.activeOperation() != null) throw InputDraftException.conflict("DRAFT_BUSY", draft.revision());
        if (draft.revision() != request.baseRevision()) throw InputDraftException.conflict("REVISION_CONFLICT", draft.revision());
        LocalDateTime now = candidate.expires().minusSeconds(600);
        sql.update("""
                INSERT INTO tmd_mdl_inptdrftprcsl(prcs_id,cust_cpny_no,usr_no,drft_id,req_id,prcs_type_cd,usr_role_cd,base_rvsn_no,
                    prcs_stat_cd,json_rvsn_no,req_json,expr_dtm,crt_dtm,mod_dtm,crt_usr_logn_id,mod_usr_logn_id)
                VALUES(?,?,?,?,?,'FINALIZE',?,?,'QUEUED',2,?,?,?,?,?,?)
                """, candidate.id(), company, user, id, request.requestId(), role, request.baseRevision(), candidate.request(), candidate.expires(), now, now, actor, actor);
        one(sql.update("UPDATE tmd_mdl_inptdrftm SET actv_prcs_id=?,mod_usr_logn_id=?,mod_dtm=CURRENT_TIMESTAMP WHERE drft_id=? AND drft_stat_cd='DRAFT' AND actv_prcs_id IS NULL AND rvsn_no=?",
                candidate.id(), actor, id, request.baseRevision()));
        return common.operation(id, candidate.id(), false);
    }

    Operation claim(String draftId, String operationId, String token) {
        Draft draft = common.lockForRecovery(draftId);
        Operation op = finalization(draftId, operationId);
        if (!op.status().equals("QUEUED") || !current(draft, op) || !common.databaseTime().isBefore(op.expires())) return null;
        one(sql.update("UPDATE tmd_mdl_inptdrftprcsl SET prcs_stat_cd='RUNNING',try_tokn=?,bgn_dtm=CURRENT_TIMESTAMP(6),mod_dtm=CURRENT_TIMESTAMP WHERE prcs_id=? AND prcs_stat_cd='QUEUED' AND expr_dtm>CURRENT_TIMESTAMP(6)", token, operationId));
        return common.operation(draftId, operationId, false);
    }

    Job prepareClaimed(String draftId, String operationId, String token) {
        Draft draft = common.lockForRecovery(draftId);
        Operation op = finalization(draftId, operationId);
        if (!op.status().equals("RUNNING") || !Objects.equals(token, op.token()) || !current(draft, op)
                || !common.databaseTime().isBefore(op.expires())) return null;
        return verify(draft, op);
    }

    private Job verify(Draft draft, Operation op) {
        WorkerOwner owner = common.workerOwner(draft, op);
        if(!draft.uploaded())common.caseAccess(draft.caseId(), true);
        ModelInputDraftService.Source source = Objects.requireNonNull(basis.verify(draft, op));
        if (!Objects.equals(source.caseId(),draft.caseId()) || source.catalog().templateId() != draft.templateId()
                || !Objects.equals(draft.sourceVersion(),source.version())) throw InputDraftException.conflict("SOURCE_CHANGED", draft.revision());
        Map<String, Long> actual = new HashMap<>();
        for (FileRef file : source.files()) {
            if (!ROLES.contains(file.role()) || actual.put(file.role(), file.fileNo()) != null || (!draft.uploaded()&&(file.fileNo()==null||file.fileNo()<=0))) {
                throw InputDraftException.internal();
            }
        }
        if(actual.size()!=4)throw InputDraftException.conflict("FILES_INCOMPLETE",draft.revision());
        if (!draft.uploaded()&&!actual.equals(common.fixedFiles(draft.id()))) throw InputDraftException.conflict("SOURCE_CHANGED", draft.revision());
        List<Revision> chain = common.revisions(draft.id());
        var rows=common.logicalRows(draft);
        var editing=new InputDraftEditingBasis(draft,source,chain,rows.identities(),rows.positions());
        editing.history(draft.revision(),json);
        return new Job(draft, op, owner, List.copyOf(source.files()), List.copyOf(chain),editing);
    }

    private Job running(Job expected) {
        Draft draft = common.lockForRecovery(expected.draft().id());
        Operation op = finalization(draft.id(), expected.operation().id());
        if (!op.status().equals("RUNNING") || !Objects.equals(op.token(), expected.operation().token())
                || !current(draft, op) || !common.databaseTime().isBefore(op.expires())) {
            throw InputDraftException.conflict("DRAFT_NOT_EDITABLE", draft.revision());
        }
        if (draft.company() != expected.draft().company() || draft.user() != expected.draft().user()
                || !Objects.equals(draft.caseId(),expected.draft().caseId()) || draft.templateId() != expected.draft().templateId()
                || !Objects.equals(draft.sourceVersion(),expected.draft().sourceVersion())
                || !Objects.equals(draft.baselineId(),expected.draft().baselineId())) throw InputDraftException.internal();
        return new Job(draft, op, expected.owner(), expected.files(), expected.revisions(),expected.basis());
    }

    void prepare(Job job, List<Reference> refs) {
        running(job);
        if (refs.size() != 4) throw InputDraftException.internal();
        for (int index = 0; index < ROLES.size(); index++) {
            String role = ROLES.get(index);
            refs.get(index).validate(job.operation(), role, job.owner());
            sql.update("""
                    INSERT INTO tmd_mdl_inptfnsvfiled(prcs_id,try_tokn,mdl_file_cd,strg_ref_json,prcs_stat_cd,crt_usr_logn_id,mod_usr_logn_id)
                    VALUES(?,?,?,?,'PREPARED',?,?)
                    """, job.operation().id(), job.operation().token(), role, encodeRef(refs.get(index)), job.operation().actor(), job.operation().actor());
        }
    }

    void content(Job job, String role, Reference ref) {
        running(job);
        if (ref.size() == null || ref.size() <= 0 || ref.size() > 300L * 1024 * 1024
                || ref.sha256() == null || !ref.sha256().matches("[0-9a-f]{64}")) throw InputDraftException.internal();
        one(sql.update("""
                UPDATE tmd_mdl_inptfnsvfiled SET strg_ref_json=?,file_size=?,mod_dtm=CURRENT_TIMESTAMP
                WHERE prcs_id=? AND try_tokn=? AND mdl_file_cd=? AND prcs_stat_cd='PREPARED' AND file_size IS NULL
                """, encodeRef(ref), ref.size(), job.operation().id(), job.operation().token(), role));
    }

    void uploaded(Job job, String role) {
        running(job);
        one(sql.update("""
                UPDATE tmd_mdl_inptfnsvfiled SET prcs_stat_cd='UPLOADED',mod_dtm=CURRENT_TIMESTAMP
                WHERE prcs_id=? AND try_tokn=? AND mdl_file_cd=? AND prcs_stat_cd='PREPARED' AND file_size IS NOT NULL
                """, job.operation().id(), job.operation().token(), role));
    }

    Result commit(Job expected) {
        Job live = running(expected);
        live = verify(live.draft(), live.operation()); // 현재 권한/고정 원본/발행/성공 chain을 같은 master 확정 안에서 재검사
        if (!live.files().equals(expected.files()) || !live.owner().equals(expected.owner())) throw InputDraftException.internal();
        Request request = readRequest(live.operation());
        List<Prepared> prepared = new ArrayList<>(prepared(live.operation().id(), live.operation().token()));
        prepared.sort(java.util.Comparator.comparingInt(item -> ROLES.indexOf(item.role())));
        if (prepared.size() != 4 || !prepared.stream().allMatch(p -> p.status().equals("UPLOADED") && p.fileNo() == null
                && p.ref().size() != null && p.ref().sha256() != null)) throw InputDraftException.internal();
        ScenarioCase pending = ScenarioCase.createPending(live.draft().user(), request.title(), request.description(), live.operation().actor());
        long caseId = insert("""
                INSERT INTO tmd_mdl_scnrcasem(usr_no,exec_type_cd,case_titl_nm,case_expn,case_stat_cd,crt_usr_logn_id,mod_usr_logn_id)
                VALUES(?,?,?,?,?,?,?)
                """, pending.getUserNo(), pending.getExecType().name(), pending.getTitle(), pending.getDescription(), pending.getStatus().name(),
                live.operation().actor(), live.operation().actor());
        List<SavedFile> saved = new ArrayList<>();
        for (Prepared item : prepared) {
            Reference ref = item.ref();
            String storageFileName = ref.key().substring(ref.key().lastIndexOf('/') + 1);
            UploadedFile file = UploadedFile.of(ref.name(), storageFileName, ref.location(), ref.size(), ref.name().endsWith(".xlsx")?"xlsx":"xlsm", ref.mime(),
                    "99991231", Math.toIntExact(caseId), "SCNRCASE", null);
            long fileNo = insert("""
                    INSERT INTO tcm_filel(org_file_nm,strg_file_nm,strg_path_nm,file_size,filex_nm,mime_type_nm,file_vald_end_dt,
                        file_rel_obj_dstg_no,file_rel_obj_spt_cd,img_conv_cmpt_yn,crt_usr_logn_id,mod_usr_logn_id)
                    VALUES(?,?,?,?,?,?,?,?,?,'N',?,?)
                    """, file.getOriginalFileNm(), file.getStorageFileNm(), file.getStoragePathNm(), file.getFileSize(), file.getFileExtNm(),
                    file.getMimeTypeNm(), file.getFileValidEndDt(), file.getFileRelationshipObjectDistinguishNo(), file.getFileRelationshipObjectSeparateCd(),
                    live.operation().actor(), live.operation().actor());
            saved.add(new SavedFile(item.role(), fileNo));
            one(sql.update("UPDATE tmd_mdl_inptfnsvfiled SET prcs_stat_cd='COMMITTED',file_no=?,mod_usr_logn_id=?,mod_dtm=CURRENT_TIMESTAMP WHERE prcs_id=? AND try_tokn=? AND mdl_file_cd=? AND prcs_stat_cd='UPLOADED' AND file_no IS NULL",
                    fileNo, live.operation().actor(), live.operation().id(), live.operation().token(), item.role()));
        }
        sql.update("INSERT INTO tmd_mdl_scnrcasetmpltd(case_id,tmplt_id,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,?,?)",
                caseId, live.draft().templateId(), live.operation().actor(), live.operation().actor());
        common.queueTableSnapshots(live.owner().company(),live.owner().user(),live.owner().companyCode(),caseId,live.operation().actor());
        Result result = new Result(caseId, List.copyOf(saved));
        one(sql.update("""
                UPDATE tmd_mdl_inptdrftprcsl SET prcs_stat_cd='SUCCEEDED',rslt_json=?,end_dtm=CURRENT_TIMESTAMP(6),mod_dtm=CURRENT_TIMESTAMP,mod_usr_logn_id=?
                WHERE prcs_id=? AND prcs_stat_cd='RUNNING' AND try_tokn=? AND expr_dtm>CURRENT_TIMESTAMP(6)
                """, encode(result), live.operation().actor(), live.operation().id(), live.operation().token()));
        one(sql.update("""
                UPDATE tmd_mdl_inptdrftm SET drft_stat_cd='FINALIZED',fnsv_case_id=?,actv_prcs_id=NULL,mod_usr_logn_id=?,mod_dtm=CURRENT_TIMESTAMP
                WHERE drft_id=? AND drft_stat_cd='DRAFT' AND rvsn_no=? AND actv_prcs_id=?
                """, caseId, live.operation().actor(), live.draft().id(), live.operation().baseRevision(), live.operation().id()));
        if(live.draft().uploaded())sql.update("UPDATE tmd_mdl_inptupldactvm SET drft_id=NULL WHERE cust_cpny_no=? AND usr_no=? AND drft_id=?",live.draft().company(),live.draft().user(),live.draft().id());
        return result;
    }

    Operation recoverStopped(String id, String operationId, String token) {
        return failStopped(id, operationId, token, "PERSISTENCE_FAILED");
    }

    Operation failStopped(String id, String operationId, String token, String reason) {
        if (!Set.of("PERSISTENCE_FAILED", "CALCULATION_FAILED", "STRUCTURE_VALIDATION_FAILED", "PRESERVATION_FAILED",
                "VALIDATION_FAILED", "SOURCE_CHANGED", "POLICY_INCOMPLETE", "POLICY_UNSUPPORTED", "TIMEOUT").contains(reason)) {
            throw InputDraftException.internal();
        }
        Draft draft = common.lockForRecovery(id);
        Operation op = finalization(id, operationId);
        if (op.terminal()) {
            if (op.status().equals("SUCCEEDED") && (!draft.status().equals("FINALIZED") || draft.finalizedCase() == null)) throw InputDraftException.internal();
            return op;
        }
        if (!op.status().equals("RUNNING") || !Objects.equals(token, op.token()) || !current(draft, op)) throw InputDraftException.internal();
        return fail(draft, op, common.databaseTime().isBefore(op.expires()) ? reason : "TIMEOUT");
    }

    Operation expireQueued(String id, String operationId) {
        Draft draft = common.lockForRecovery(id);
        Operation op = finalization(id, operationId);
        if (!op.status().equals("QUEUED") || common.databaseTime().isBefore(op.expires())) return op;
        if (!current(draft, op)) throw InputDraftException.internal();
        return fail(draft, op, "TIMEOUT");
    }

    private Operation fail(Draft draft, Operation op, String reason) {
        String failure = encode(Map.of("reason", reason, "currentRevision", draft.revision(), "errors", List.of()));
        one(sql.update("UPDATE tmd_mdl_inptdrftprcsl SET prcs_stat_cd='FAILED',fail_json=?,end_dtm=CURRENT_TIMESTAMP(6),mod_dtm=CURRENT_TIMESTAMP,mod_usr_logn_id=? WHERE prcs_id=? AND prcs_stat_cd=?",
                failure, op.actor(), op.id(), op.status()));
        one(sql.update("UPDATE tmd_mdl_inptdrftm SET actv_prcs_id=NULL,mod_dtm=CURRENT_TIMESTAMP,mod_usr_logn_id=? WHERE drft_id=? AND actv_prcs_id=? AND drft_stat_cd='DRAFT' AND rvsn_no=?",
                op.actor(), draft.id(), op.id(), draft.revision()));
        sql.update("UPDATE tmd_mdl_inptfnsvfiled SET prcs_stat_cd='CLEANUP_PENDING',mod_dtm=CURRENT_TIMESTAMP WHERE prcs_id=? AND try_tokn=? AND prcs_stat_cd IN ('PREPARED','UPLOADED') AND file_no IS NULL", op.id(), op.token());
        return common.operation(draft.id(), op.id(), false);
    }

    List<Prepared> cleanupCandidates(String id, String operationId, String token) {
        Draft draft = common.lockForRecovery(id);
        Operation op = finalization(id, operationId);
        if (!Objects.equals(token, op.token()) || !op.status().equals("FAILED")) return List.of();
        return prepared(operationId, token).stream().filter(p -> p.fileNo() == null && p.status().equals("CLEANUP_PENDING")).toList();
    }

    void observeAbsent(String id, String operationId, String token, String role, boolean absent) {
        Prepared pending = cleanupCandidates(id, operationId, token).stream().filter(p -> p.role().equals(role)).findFirst().orElse(null);
        if (pending == null) return;
        LocalDateTime now = common.databaseTime();
        String before = pending.ref().absentSince();
        boolean cleaned = absent && before != null && now.isAfter(LocalDateTime.parse(before));
        Reference next = pending.ref().observed(absent ? (before == null ? now.toString() : before) : null);
        one(sql.update("UPDATE tmd_mdl_inptfnsvfiled SET strg_ref_json=?,prcs_stat_cd=?,mod_dtm=CURRENT_TIMESTAMP WHERE prcs_id=? AND try_tokn=? AND mdl_file_cd=? AND prcs_stat_cd='CLEANUP_PENDING' AND file_no IS NULL",
                encodeRef(next), cleaned ? "CLEANED" : "CLEANUP_PENDING", operationId, token, role));
    }

    private List<Prepared> prepared(String operation, String token) {
        requireTransaction();
        return sql.query("SELECT * FROM tmd_mdl_inptfnsvfiled WHERE prcs_id=? AND try_tokn=? AND del_yn='N' ORDER BY mdl_file_cd FOR UPDATE",
                (rs, n) -> new Prepared(rs.getString("mdl_file_cd"), rs.getString("prcs_stat_cd"), readRef(rs.getString("strg_ref_json")), rs.getObject("file_no", Long.class)), operation, token);
    }

    private Operation finalization(String draft, String operation) {
        Operation op = common.operation(draft, operation, true);
        if (!op.type().equals("FINALIZE")) throw InputDraftException.missing();
        if (op.schemaVersion() != 2) throw InputDraftException.internal();
        readRequest(op);
        return op;
    }

    private Request readRequest(Operation op) {
        JsonNode node = json.payload(op.request());
        if (!node.isObject() || node.size() != 4 || !node.has("description")
                || !node.path("requestId").isTextual() || !node.path("baseRevision").isIntegralNumber()
                || !node.path("baseRevision").canConvertToLong() || !node.path("title").isTextual()
                || (!node.path("description").isNull() && !node.path("description").isTextual())) throw InputDraftException.internal();
        Request result = new Request(node.path("requestId").asText(), node.path("baseRevision").longValue(), node.path("title").asText(),
                node.path("description").isNull() ? null : node.path("description").asText());
        if (!result.requestId().equals(op.requestId()) || result.baseRevision() != op.baseRevision()) throw InputDraftException.internal();
        return result;
    }

    private static boolean same(Request left, Request right) {
        return left.baseRevision() == right.baseRevision() && left.title().equals(right.title()) && Objects.equals(left.description(), right.description());
    }
    private static boolean current(Draft draft, Operation op) {
        return draft.status().equals("DRAFT") && op.id().equals(draft.activeOperation()) && draft.revision() == op.baseRevision();
    }
    private String encode(Object value) {
        byte[] bytes = json.bytes(value);
        if (bytes.length > InputDraftJson.BODY_LIMIT) throw InputDraftException.internal();
        return new String(bytes, StandardCharsets.UTF_8);
    }
    private String encodeRef(Reference ref) {
        String value = encode(ref);
        if (value.getBytes(StandardCharsets.UTF_8).length > 16384) throw InputDraftException.internal();
        return value;
    }
    private Reference readRef(String text) {
        if (text.getBytes(StandardCharsets.UTF_8).length > 16384) throw InputDraftException.internal();
        JsonNode value = json.payload(text);
        Set<String> fields = Set.of("bucket", "key", "location", "name", "mime", "company", "size", "sha256", "absentSince");
        if (!value.isObject() || value.size() != fields.size()) throw InputDraftException.internal();
        for (String field : fields) if (!value.has(field) || (field.equals("size") ? !value.get(field).isNull() && !value.get(field).isIntegralNumber()
                : !value.get(field).isNull() && !value.get(field).isTextual())) throw InputDraftException.internal();
        return new Reference(value.path("bucket").asText(null), value.path("key").asText(null), value.path("location").asText(null),
                value.path("name").asText(null), value.path("mime").asText(null), value.path("company").asText(null),
                value.path("size").isNull() ? null : value.path("size").longValue(), value.path("sha256").asText(null), value.path("absentSince").asText(null));
    }
    private long insert(String statement, Object... arguments) {
        var key = new GeneratedKeyHolder();
        one(sql.update(connection -> {
            var query = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < arguments.length; i++) query.setObject(i + 1, arguments[i]);
            return query;
        }, key));
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    private static void one(int count) { if (count != 1) throw InputDraftException.internal(); }
    private static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("master transaction required");
    }
}
