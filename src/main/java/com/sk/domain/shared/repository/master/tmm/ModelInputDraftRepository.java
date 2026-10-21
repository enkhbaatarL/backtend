package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.mi.service.fundamentalmodel.input.InputDraftException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** master transaction에 참여하는 명시적 SQL. 원본 projection과 초기 행 대응 bulk 저장에 사용한다. */
@Slf4j
@Repository
public class ModelInputDraftRepository {
    private static final int ORIGINAL_ROW_INSERT_SIZE = 500;
    private final JdbcTemplate jdbc;
    public ModelInputDraftRepository(@Qualifier("masterDataSource") DataSource master) { jdbc=new JdbcTemplate(master); }
    public record FileRef(String role,Long fileNo,String name,String location,long baseline,String sha256,Long size) {
        public FileRef(String role,long fileNo,String name,String location){this(role,fileNo,name,location,0,null,null);}
    }
    public record Scope(int startRow,int endRow,int startColumn,int endColumn) {
        public boolean contains(int row,int col) { return row>=startRow&&row<=endRow&&col>=startColumn&&col<=endColumn; }
    }
    public record TableRef(long id,String role,String sheet,String name,String description,int startRow,int endRow,
            int startColumn,int endColumn,int headerStart,int headerEnd,int dataStart,boolean rowAdd,boolean columnAdd,
            String definition,List<Scope> scopes) {
        public String key() { return role+"/"+sheet+"/"+name; }
    }
    public record Catalog(long templateId,int version,List<String> roles,List<TableRef> tables) {}
    public record Draft(String id,int company,int user,Long caseId,long templateId,String sourceVersion,String status,
            long revision,String activeOperation,Long finalizedCase) {
        public Draft(String id,int company,int user,long caseId,long templateId,String sourceVersion,String status,long revision,String activeOperation,Long finalizedCase){this(id,company,user,Long.valueOf(caseId),templateId,sourceVersion,status,revision,activeOperation,finalizedCase);}
        public boolean uploaded(){return caseId==null;}
    }
    public record RowRef(long table,int row,String id) {}
    public List<Long> findActiveTemplateIdsForUpdate(String modelCode) {
        requireTransaction();
        return jdbc.queryForList("""
            SELECT tmplt_id FROM tmd_mdl_inpttmpltm
            WHERE mdl_spt_cd=? AND tmplt_stat_cd='ACTIVE' AND del_yn='N'
            FOR UPDATE
            """, Long.class, modelCode);
    }

    public void bindCaseTemplate(long caseId, long templateId, String actor) {
        requireTransaction();
        jdbc.update("""
            INSERT INTO tmd_mdl_scnrcasetmpltd(case_id,tmplt_id,crt_usr_logn_id,mod_usr_logn_id)
            VALUES(?,?,?,?)
            """, caseId, templateId, actor, actor);
    }

    public void caseAccess(long id, boolean lock) {
        List<String> types=jdbc.query("SELECT exec_type_cd FROM tmd_mdl_scnrcasem WHERE case_id=? AND del_yn='N'"+(lock?" FOR UPDATE":""),
            (rs,n)->rs.getString(1),id);
        if(types.size()!=1||!List.of("INDEPENDENT","MI_BASE").contains(types.get(0))) throw InputDraftException.missing();
    }
    public List<FileRef> files(long caseId) {
        return jdbc.query("""
            SELECT file_no,org_file_nm,strg_path_nm FROM tcm_filel
            WHERE file_rel_obj_dstg_no=? AND file_rel_obj_spt_cd='SCNRCASE' AND del_yn='N'
              AND (file_vald_end_dt IS NULL OR file_vald_end_dt>=?) ORDER BY file_no
            """,(rs,n)->new FileRef(null,rs.getLong(1),rs.getString(2),rs.getString(3)),
            Math.toIntExact(caseId),java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
    }
    public Catalog catalog(long caseId) {
        List<long[]> root=jdbc.query("""
            SELECT t.tmplt_id,t.tmplt_ver_no FROM tmd_mdl_scnrcasetmpltd c
            JOIN tmd_mdl_inpttmpltm t ON t.tmplt_id=c.tmplt_id
            WHERE c.case_id=? AND c.del_yn='N' AND t.del_yn='N' AND t.tmplt_stat_cd IN ('ACTIVE','DEPRECATED')
            """,(rs,n)->new long[]{rs.getLong(1),rs.getInt(2)},caseId);
        if(root.size()!=1) throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        return catalogTemplate(root.get(0)[0],(int)root.get(0)[1]);
    }
    public Catalog catalogTemplate(long template,int version) {
        List<String> roles=jdbc.query("SELECT mdl_file_cd FROM tmd_mdl_inptfiled WHERE tmplt_id=? AND del_yn='N' ORDER BY file_ord",(rs,n)->rs.getString(1),template);
        Map<Long,List<Scope>> scopes=new LinkedHashMap<>();
        jdbc.query("""
            SELECT m.inpt_tbl_id,m.bgn_row_no,m.end_row_no,m.bgn_col_no,m.end_col_no
            FROM tmd_mdl_inptmodscopd m JOIN tmd_mdl_inpttbld t ON t.inpt_tbl_id=m.inpt_tbl_id
            JOIN tmd_mdl_inptshtd s ON s.inpt_sht_id=t.inpt_sht_id JOIN tmd_mdl_inptfiled f ON f.inpt_file_id=s.inpt_file_id
            WHERE f.tmplt_id=? AND f.del_yn='N' AND s.del_yn='N' AND s.sht_stat_cd='VISIBLE' AND t.del_yn='N' AND m.del_yn='N'
            ORDER BY m.inpt_tbl_id,m.mod_scop_ord
            """,rs->{scopes.computeIfAbsent(rs.getLong(1),k->new ArrayList<>()).add(new Scope(rs.getInt(2),rs.getInt(3),rs.getInt(4),rs.getInt(5)));},template);
        List<TableRef> tables=jdbc.query("""
            SELECT t.*,s.sht_nm,f.mdl_file_cd FROM tmd_mdl_inpttbld t
            JOIN tmd_mdl_inptshtd s ON s.inpt_sht_id=t.inpt_sht_id JOIN tmd_mdl_inptfiled f ON f.inpt_file_id=s.inpt_file_id
            WHERE f.tmplt_id=? AND f.del_yn='N' AND s.del_yn='N' AND s.sht_stat_cd='VISIBLE' AND t.del_yn='N'
            ORDER BY f.file_ord,s.sht_ord,t.tbl_ord
            """,(rs,n)->new TableRef(rs.getLong("inpt_tbl_id"),rs.getString("mdl_file_cd"),rs.getString("sht_nm"),rs.getString("inpt_tbl_nm"),
            rs.getString("inpt_tbl_expn"),rs.getInt("tbl_bgn_row_no"),rs.getInt("tbl_end_row_no"),rs.getInt("tbl_bgn_col_no"),rs.getInt("tbl_end_col_no"),
            rs.getInt("hdr_bgn_row_no"),rs.getInt("hdr_end_row_no"),rs.getInt("data_bgn_row_no"),"Y".equals(rs.getString("row_add_psbl_yn")),
            "Y".equals(rs.getString("col_add_psbl_yn")),rs.getString("qry_json"),List.copyOf(scopes.getOrDefault(rs.getLong("inpt_tbl_id"),List.of()))),template);
        return new Catalog(template,version,List.copyOf(roles),List.copyOf(tables));
    }
    private Draft draft(ResultSet rs,int ignored)throws SQLException {
        return new Draft(rs.getString("drft_id"),rs.getInt("cust_cpny_no"),rs.getInt("usr_no"),rs.getObject("org_case_id",Long.class),
            rs.getLong("tmplt_id"),rs.getString("org_rvsn_val"),rs.getString("drft_stat_cd"),rs.getLong("rvsn_no"),
            rs.getString("actv_prcs_id"),rs.getObject("fnsv_case_id",Long.class));
    }
    public Draft active(int company,int user,long caseId) {
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftm WHERE cust_cpny_no=? AND usr_no=? AND org_case_id=? AND drft_stat_cd='DRAFT' AND del_yn='N'",
            this::draft,company,user,caseId).stream().findFirst().orElse(null);
    }

    public Map<Long, String> findActiveDraftIds(int company, int user, List<Long> caseIds) {
        if (caseIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", Collections.nCopies(caseIds.size(), "?"));
        List<Object> parameters = new ArrayList<>();
        parameters.add(company);
        parameters.add(user);
        parameters.addAll(caseIds);
        return jdbc.query("""
            SELECT org_case_id, drft_id FROM tmd_mdl_inptdrftm
            WHERE cust_cpny_no=? AND usr_no=? AND drft_stat_cd='DRAFT' AND del_yn='N'
              AND org_case_id IN (%s)
            """.formatted(placeholders), rs -> {
                Map<Long, String> draftIds = new LinkedHashMap<>();
                while (rs.next()) {
                    draftIds.put(rs.getLong("org_case_id"), rs.getString("drft_id"));
                }
                return draftIds;
            }, parameters.toArray());
    }

    public Draft owned(String id,int company,int user) {
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftm WHERE drft_id=? AND cust_cpny_no=? AND usr_no=? AND del_yn='N'",
            this::draft,id,company,user).stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    /** 접수/복구는 항상 Draft → 요청 순서로 잠근다. 호출 transaction은 master여야 한다. */
    public Draft lockOwned(String id,int company,int user) {
        requireTransaction();
        lockUploadOwner(owned(id,company,user));
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftm WHERE drft_id=? AND cust_cpny_no=? AND usr_no=? AND del_yn='N' FOR UPDATE",
            this::draft,id,company,user).stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    private Draft lockDraft(String id) {
        requireTransaction();
        jdbc.query("SELECT * FROM tmd_mdl_inptdrftm WHERE drft_id=? AND del_yn='N'",this::draft,id).stream().findFirst().ifPresent(this::lockUploadOwner);
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftm WHERE drft_id=? AND del_yn='N' FOR UPDATE",this::draft,id)
            .stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    public Draft lockForRecovery(String id){return lockDraft(id);}
    private void lockUploadOwner(Draft draft) {
        if(draft.uploaded())jdbc.query("SELECT cust_cpny_no FROM tmd_mdl_inptupldactvm WHERE cust_cpny_no=? AND usr_no=? FOR UPDATE",(rs,n)->rs.getInt(1),draft.company(),draft.user());
    }
    private static void requireTransaction() {
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("master transaction required");
    }
    public record Operation(String id,String draftId,String requestId,String type,String role,long baseRevision,
        String status,int schemaVersion,String request,String token,LocalDateTime expires,String result,String failure,String actor) {
        public boolean terminal(){return status.equals("SUCCEEDED")||status.equals("FAILED");}
    }
    private Operation operation(ResultSet rs,int ignored)throws SQLException {
        return new Operation(rs.getString("prcs_id"),rs.getString("drft_id"),rs.getString("req_id"),rs.getString("prcs_type_cd"),
            rs.getString("usr_role_cd"),rs.getLong("base_rvsn_no"),rs.getString("prcs_stat_cd"),rs.getInt("json_rvsn_no"),
            rs.getString("req_json"),rs.getString("try_tokn"),rs.getTimestamp("expr_dtm").toLocalDateTime(),rs.getString("rslt_json"),rs.getString("fail_json"),rs.getString("crt_usr_logn_id"));
    }
    public Operation request(String draftId,String requestId) {
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftprcsl WHERE drft_id=? AND req_id=?",this::operation,draftId,requestId).stream().findFirst().orElse(null);
    }
    public Operation operation(String draftId,String operationId,boolean lock) {
        if(lock)requireTransaction();
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftprcsl WHERE drft_id=? AND prcs_id=?"+(lock?" FOR UPDATE":""),this::operation,draftId,operationId)
            .stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    public LocalDateTime databaseTime(){return jdbc.queryForObject("SELECT CURRENT_TIMESTAMP(6)",LocalDateTime.class);}
    /** 웹 실행기의 저장 결과 재확인. UPLOAD_CREATE는 아직 draft가 없을 수 있다. */
    public Operation workerOperation(String operationId) {
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftprcsl WHERE prcs_id=?",this::operation,operationId)
                .stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    /** 종료 증거를 가진 실행기만 사용한다. 실패 확정 직후/정리 중 재시작도 포함한다. */
    public List<Operation> stoppedWorkerOperations() {
        return jdbc.query("""
            SELECT p.* FROM tmd_mdl_inptdrftprcsl p WHERE p.prcs_stat_cd='RUNNING'
            OR (p.prcs_type_cd='FINALIZE' AND p.prcs_stat_cd='FAILED' AND EXISTS (
                SELECT 1 FROM tmd_mdl_inptfnsvfiled f WHERE f.prcs_id=p.prcs_id AND f.try_tokn=p.try_tokn
                    AND f.prcs_stat_cd='CLEANUP_PENDING'))
            ORDER BY p.expr_dtm,p.prcs_id LIMIT 100
            """,this::operation);
    }
    public Operation enqueueChange(Draft draft,String requestId,long baseRevision,String request,String role,String actor) {
        Draft current=lockDraft(draft.id());
        if(!current.status().equals("DRAFT"))throw InputDraftException.conflict("DRAFT_NOT_EDITABLE",current.revision());
        if(current.activeOperation()!=null)throw InputDraftException.conflict("DRAFT_BUSY",current.revision());
        if(current.revision()!=baseRevision)throw InputDraftException.conflict("REVISION_CONFLICT",current.revision());
        // 감사 시간은 초 단위다. 같은 값을 기한의 기준으로 써 DB 반올림/CHECK 불일치를 막는다.
        LocalDateTime now=databaseTime().withNano(0);String id=UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO tmd_mdl_inptdrftprcsl(prcs_id,cust_cpny_no,usr_no,drft_id,req_id,prcs_type_cd,usr_role_cd,base_rvsn_no,prcs_stat_cd,json_rvsn_no,req_json,expr_dtm,crt_dtm,mod_dtm,crt_usr_logn_id,mod_usr_logn_id)
            VALUES(?,?,?,?,?,'CHANGE',?,?,'QUEUED',2,?,?,?,?,?,?)
            """,id,current.company(),current.user(),current.id(),requestId,role,baseRevision,request,now.plusSeconds(120),now,now,actor,actor);
        int changed=jdbc.update("UPDATE tmd_mdl_inptdrftm SET actv_prcs_id=?,mod_usr_logn_id=?,mod_dtm=? WHERE drft_id=? AND actv_prcs_id IS NULL AND rvsn_no=? AND drft_stat_cd='DRAFT'",id,actor,now,current.id(),baseRevision);
        if(changed!=1)throw InputDraftException.internal();
        return operation(current.id(),id,false);
    }
    /** 후속 격리 worker가 DB에서 같은 QUEUED를 찾는 경계. 메모리 queue는 근거가 아니다. */
    public List<Operation> queuedChanges() { return queuedOperations(false); }
    public List<Operation> queuedOperations(boolean finalizations) {
        return jdbc.query("""
            SELECT p.* FROM tmd_mdl_inptdrftprcsl p
            JOIN tmd_mdl_inptdrftm d ON d.drft_id=p.drft_id JOIN tmd_mdl_inpttmpltm t ON t.tmplt_id=d.tmplt_id
            WHERE p.prcs_stat_cd='QUEUED' AND (p.prcs_type_cd='CHANGE' OR (? AND p.prcs_type_cd='FINALIZE'))
            ORDER BY p.expr_dtm,p.prcs_id LIMIT 100
            """,this::operation,finalizations);
    }

    /** 성공 저장 결과에 함께 기록한 실행 대기만 찾는다. 과거 성공 기록에는 이 필드가 없다. */
    public List<Operation> queuedFinalizationExecutions() {
        return jdbc.query("""
            SELECT p.* FROM tmd_mdl_inptdrftprcsl p
            WHERE p.prcs_type_cd='FINALIZE' AND p.prcs_stat_cd='SUCCEEDED'
              AND p.rslt_json LIKE ?
            ORDER BY p.end_dtm,p.prcs_id LIMIT 100
            """, this::operation, "%\"executionStatus\"%\"QUEUED\"%");
    }

    public record FinalizationCase(long caseId, String status, String runId) {}

    /** 요청 잠금 뒤 최종 Case도 잠근다. 수동 실행과 자동 실행이 같은 Case를 선점한다. */
    public FinalizationCase lockFinalizationCase(Draft draft) {
        requireTransaction();
        if (!draft.status().equals("FINALIZED") || draft.finalizedCase() == null) throw InputDraftException.internal();
        return jdbc.query("""
            SELECT case_id,case_stat_cd,last_run_id FROM tmd_mdl_scnrcasem
            WHERE case_id=? AND usr_no=? AND exec_type_cd='INDEPENDENT' AND del_yn='N' FOR UPDATE
            """, (rs,n) -> new FinalizationCase(rs.getLong(1),rs.getString(2),rs.getString(3)),
            draft.finalizedCase(),draft.user()).stream().findFirst().orElseThrow(InputDraftException::missing);
    }

    public void completeFinalizationExecution(Operation operation, String result) {
        requireTransaction();
        if (result.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1_048_576) throw InputDraftException.internal();
        int changed = jdbc.update("""
            UPDATE tmd_mdl_inptdrftprcsl SET rslt_json=?,mod_dtm=CURRENT_TIMESTAMP
            WHERE prcs_id=? AND drft_id=? AND prcs_type_cd='FINALIZE' AND prcs_stat_cd='SUCCEEDED' AND rslt_json=?
            """, result,operation.id(),operation.draftId(),operation.result());
        if (changed != 1) throw InputDraftException.internal();
    }

    public record FinalizationRun(String runId, String status, String failureReason) {}

    public FinalizationRun finalizationRun(long caseId, String runId) {
        return jdbc.query("""
            SELECT run_id,exec_stat_cd,fail_rsn_cd FROM tmd_mdl_scnrcaseh
            WHERE case_id=? AND run_id=? AND del_yn='N'
            """, (rs,n) -> new FinalizationRun(rs.getString(1),rs.getString(2),rs.getString(3)),
            caseId,runId).stream().findFirst().orElseThrow(InputDraftException::internal);
    }
    public Operation claimChange(String draftId,String operationId) {
        return claimChange(draftId,operationId,UUID.randomUUID().toString());
    }
    /** marker를 먼저 fsync한 서버 토큰만 전달한다. HTTP 입력으로 받지 않는다. */
    public Operation claimChange(String draftId,String operationId,String token) {
        if(token==null||!UUID.fromString(token).toString().equals(token))throw InputDraftException.internal();
        Draft draft=lockDraft(draftId);Operation op=operation(draftId,operationId,true);
        if(op.schemaVersion()!=2||!op.type().equals("CHANGE"))throw InputDraftException.internal();
        if(!op.status().equals("QUEUED")||!draft.status().equals("DRAFT")||!operationId.equals(draft.activeOperation())
            ||draft.revision()!=op.baseRevision()||!databaseTime().isBefore(op.expires()))return null;
        int changed=jdbc.update("UPDATE tmd_mdl_inptdrftprcsl SET prcs_stat_cd='RUNNING',try_tokn=?,bgn_dtm=CURRENT_TIMESTAMP(6),mod_dtm=CURRENT_TIMESTAMP WHERE drft_id=? AND prcs_id=? AND prcs_stat_cd='QUEUED' AND expr_dtm>CURRENT_TIMESTAMP(6)",token,draftId,operationId);
        return changed==1?operation(draftId,operationId,false):null;
    }
    public record WorkerOwner(int company,int user,String companyCode,String login) {}
    /** 요청의 원래 actor/역할과 현재 DB의 회사·사용 권한을 함께 확인한다. */
    public WorkerOwner workerOwner(Draft draft,Operation op) {
        return jdbc.query("""
            SELECT d.cust_cpny_no,d.usr_no,c.cust_cpny_cd,u.usr_logn_id
            FROM tmd_mdl_inptdrftm d JOIN tmd_usrm u ON u.usr_no=d.usr_no
            JOIN tmd_custcpnym c ON c.cust_cpny_no=d.cust_cpny_no
            WHERE d.drft_id=? AND d.del_yn='N' AND u.del_yn='N' AND c.del_yn='N'
              AND u.usr_logn_id=? AND u.usr_role_cd=? AND u.usr_role_cd IN ('USER','ADMIN')
              AND ((u.usr_role_cd='USER' AND u.cust_cpny_no=d.cust_cpny_no AND c.cust_cpny_cd='SKGAS')
                OR (u.usr_role_cd='ADMIN' AND u.otcmp_svc_accs_psbl_yn='Y'))
            """,(rs,n)->new WorkerOwner(rs.getInt(1),rs.getInt(2),rs.getString(3),rs.getString(4)),
            draft.id(),op.actor(),op.role()).stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    /** 성공 기록도 이 잠금/기준 아래 같은 transaction에서 delta·행·revision과 함께 확정해야 한다. */
    public Operation lockRunningChange(String draftId,String operationId,String token) {
        Draft draft=lockDraft(draftId);Operation op=operation(draftId,operationId,true);
        return op.schemaVersion()==2&&op.type().equals("CHANGE")&&op.status().equals("RUNNING")&&token!=null&&token.equals(op.token())
            &&draft.status().equals("DRAFT")&&operationId.equals(draft.activeOperation())&&draft.revision()==op.baseRevision()
            &&databaseTime().isBefore(op.expires())?op:null;
    }
    public record Revision(long number,Long base,String operation,int schema,String delta,
        String type,String status,int operationSchema,Long operationBase,String request,String result) {}
    public record RowIdentity(long table,String id,Integer original,String operation,String client,long baseline) {
        public RowIdentity(long table,String id,Integer original,String operation,String client){this(table,id,original,operation,client,0);}
    }
    public record RowPosition(long table,String id,long begin,Long end,Integer row) {}
    public record RowWrite(long table,String id,String client,Integer row,boolean added) {}
    public record InitialRowRange(long table,int first,int last) {}
    /** revision0의 전체 저장 행을 DB에서 검증한다. 호출자는 같은 transaction의 Draft 잠금을 보유한다. */
    public void validateInitialRows(String draft,List<InitialRowRange> ranges) {
        requireTransaction();
        List<Object> parameters=new ArrayList<>();parameters.add(draft);
        parameters.add("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        List<String> allowed=new ArrayList<>();
        for(InitialRowRange range:ranges) {
            allowed.add("(m.inpt_tbl_id=? AND m.org_row_no BETWEEN ? AND ?)");
            parameters.add(range.table());parameters.add(range.first());parameters.add(range.last());
        }
        parameters.add(draft);parameters.add(draft);
        String body=allowed.isEmpty()?"1=0":String.join(" OR ",allowed);
        // 양방향 비교: 누락된 위치, 고아 위치, 추가 이력과 초기 행 위치 변경을 모두 거부한다.
        String statement="""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM tmd_mdl_inptdrftrowm m
                LEFT JOIN tmd_mdl_inptdrftrowh h ON h.row_id=m.row_id AND h.bgn_rvsn_no=0
                WHERE m.drft_id=? AND (m.del_yn<>'N' OR m.org_row_no IS NULL OR m.file_rvsn_no<>0
                    OR m.prcs_id IS NOT NULL OR m.clnt_row_id IS NOT NULL OR NOT REGEXP_LIKE(TRIM(m.row_id),?,'c')
                    OR NOT ("""+body+"""
                    ) OR h.row_id IS NULL OR h.drft_id<>m.drft_id OR h.inpt_tbl_id<>m.inpt_tbl_id
                    OR h.row_no IS NULL OR h.row_no<>m.org_row_no OR h.end_rvsn_no IS NOT NULL OR h.del_yn<>'N')
            ) OR EXISTS (
                SELECT 1 FROM tmd_mdl_inptdrftrowh h LEFT JOIN tmd_mdl_inptdrftrowm m ON m.row_id=h.row_id
                WHERE h.drft_id=? AND (m.row_id IS NULL OR m.drft_id<>h.drft_id OR m.inpt_tbl_id<>h.inpt_tbl_id
                    OR h.bgn_rvsn_no<>0 OR h.end_rvsn_no IS NOT NULL OR h.row_no IS NULL
                    OR h.row_no<>m.org_row_no OR h.del_yn<>'N')
            ) OR EXISTS (
                SELECT 1 FROM tmd_mdl_inptdrftrowm WHERE drft_id=?
                GROUP BY inpt_tbl_id,org_row_no HAVING COUNT(*)>1
            ) THEN 1 ELSE 0 END
            """;
        if(jdbc.queryForObject(statement,Integer.class,parameters.toArray())!=0)throw InputDraftException.internal();
    }
    public List<RowIdentity> initialPageRows(String draft,long table,int first,int last) {
        return jdbc.query("""
            SELECT inpt_tbl_id,row_id,org_row_no FROM tmd_mdl_inptdrftrowm
            WHERE drft_id=? AND inpt_tbl_id=? AND org_row_no BETWEEN ? AND ? ORDER BY org_row_no
            """,(rs,n)->new RowIdentity(rs.getLong(1),rs.getString(2),rs.getInt(3),null,null),draft,table,first,last);
    }
    public List<Revision> revisions(String draftId) {
        return jdbc.query("""
            SELECT r.*,p.prcs_type_cd,p.prcs_stat_cd,p.json_rvsn_no AS op_schema,
                p.base_rvsn_no AS op_base,p.req_json,p.rslt_json,p.del_yn AS op_deleted
            FROM tmd_mdl_inptdrftrvsnl r LEFT JOIN tmd_mdl_inptdrftprcsl p
                ON p.drft_id=r.drft_id AND p.prcs_id=r.prcs_id
            WHERE r.drft_id=? ORDER BY r.rvsn_no
            """,(rs,n)->{
                if(!"N".equals(rs.getString("del_yn"))||(rs.getString("prcs_id")!=null&&!"N".equals(rs.getString("op_deleted"))))throw InputDraftException.internal();
                return new Revision(rs.getLong("rvsn_no"),rs.getObject("base_rvsn_no",Long.class),rs.getString("prcs_id"),rs.getInt("json_rvsn_no"),rs.getString("dlta_json"),
                    rs.getString("prcs_type_cd"),rs.getString("prcs_stat_cd"),rs.getInt("op_schema"),rs.getObject("op_base",Long.class),rs.getString("req_json"),rs.getString("rslt_json"));
            },draftId);
    }
    public List<RowIdentity> rowIdentities(String draftId) {
        return jdbc.query("SELECT inpt_tbl_id,row_id,org_row_no,prcs_id,clnt_row_id,file_rvsn_no,del_yn FROM tmd_mdl_inptdrftrowm WHERE drft_id=? ORDER BY inpt_tbl_id,row_id",(rs,n)->{
            if(!"N".equals(rs.getString("del_yn")))throw InputDraftException.internal();
            return new RowIdentity(rs.getLong("inpt_tbl_id"),rs.getString("row_id"),rs.getObject("org_row_no",Integer.class),rs.getString("prcs_id"),rs.getString("clnt_row_id"),rs.getLong("file_rvsn_no"));
        },draftId);
    }
    public List<RowPosition> rowPositions(String draftId) {
        return jdbc.query("SELECT inpt_tbl_id,row_id,bgn_rvsn_no,end_rvsn_no,row_no,del_yn FROM tmd_mdl_inptdrftrowh WHERE drft_id=? ORDER BY row_id,bgn_rvsn_no",(rs,n)->{
            if(!"N".equals(rs.getString("del_yn")))throw InputDraftException.internal();
            return new RowPosition(rs.getLong("inpt_tbl_id"),rs.getString("row_id"),rs.getLong("bgn_rvsn_no"),rs.getObject("end_rvsn_no",Long.class),rs.getObject("row_no",Integer.class));
        },draftId);
    }
    /** 검증된 결과만 저장한다. 호출자는 같은 master transaction에서 payload/행 소속을 먼저 검사한다. */
    public boolean completeChange(String draftId,String operationId,String token,String delta,String result,List<RowWrite> rows) {
        Operation op=lockRunningChange(draftId,operationId,token);
        if(op==null)return false;
        long next=op.baseRevision()+1;
        if(next>9007199254740991L)throw InputDraftException.internal();
        jdbc.update("""
            INSERT INTO tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no,json_rvsn_no,base_rvsn_no,prcs_id,dlta_json,crt_usr_logn_id,mod_usr_logn_id)
            VALUES(?,?,2,?,?,?,?,?)
            """,draftId,next,op.baseRevision(),op.id(),delta,op.actor(),op.actor());
        for(RowWrite row:rows)if(row.added())jdbc.update("""
            INSERT INTO tmd_mdl_inptdrftrowm(row_id,drft_id,inpt_tbl_id,prcs_id,clnt_row_id,crt_usr_logn_id,mod_usr_logn_id)
            VALUES(?,?,?,?,?,?,?)
            """,row.id(),draftId,row.table(),op.id(),row.client(),op.actor(),op.actor());
        // 위치 교환/이동은 모든 이전 current를 닫은 뒤 새 위치를 넣어 generated UK를 지킨다.
        for(RowWrite row:rows)if(row.added())jdbc.update("UPDATE tmd_mdl_inptdrftrowm SET file_rvsn_no=COALESCE((SELECT u.file_rvsn_no FROM tmd_mdl_inptdrftfiled f JOIN tmd_mdl_inptupldfiled u ON u.upld_file_id=f.upld_file_id JOIN tmd_mdl_inpttbld t ON t.inpt_tbl_id=? JOIN tmd_mdl_inptshtd sh ON sh.inpt_sht_id=t.inpt_sht_id JOIN tmd_mdl_inptfiled tf ON tf.inpt_file_id=sh.inpt_file_id AND tf.mdl_file_cd=f.mdl_file_cd WHERE f.drft_id=?),0) WHERE row_id=?",row.table(),draftId,row.id());
        for(RowWrite row:rows)if(!row.added()) {
            int changed=jdbc.update("UPDATE tmd_mdl_inptdrftrowh SET end_rvsn_no=?,mod_usr_logn_id=?,mod_dtm=CURRENT_TIMESTAMP WHERE drft_id=? AND inpt_tbl_id=? AND row_id=? AND end_rvsn_no IS NULL",next,op.actor(),draftId,row.table(),row.id());
            if(changed!=1)throw InputDraftException.internal();
        }
        for(RowWrite row:rows)jdbc.update("""
            INSERT INTO tmd_mdl_inptdrftrowh(row_id,bgn_rvsn_no,drft_id,inpt_tbl_id,row_no,crt_usr_logn_id,mod_usr_logn_id)
            VALUES(?,?,?,?,?,?,?)
            """,row.id(),next,draftId,row.table(),row.row(),op.actor(),op.actor());
        int finished=jdbc.update("""
            UPDATE tmd_mdl_inptdrftprcsl SET prcs_stat_cd='SUCCEEDED',rslt_json=?,end_dtm=CURRENT_TIMESTAMP(6),mod_dtm=CURRENT_TIMESTAMP,mod_usr_logn_id=?
            WHERE drft_id=? AND prcs_id=? AND prcs_stat_cd='RUNNING' AND try_tokn=? AND expr_dtm>CURRENT_TIMESTAMP(6)
            """,result,op.actor(),draftId,op.id(),token);
        if(finished!=1)throw InputDraftException.internal(); // 늦은 완료는 위 INSERT까지 전부 rollback한다.
        int advanced=jdbc.update("""
            UPDATE tmd_mdl_inptdrftm SET rvsn_no=?,actv_prcs_id=NULL,mod_dtm=CURRENT_TIMESTAMP,mod_usr_logn_id=?
            WHERE drft_id=? AND drft_stat_cd='DRAFT' AND rvsn_no=? AND actv_prcs_id=?
            """,next,op.actor(),draftId,op.baseRevision(),op.id());
        if(advanced!=1)throw InputDraftException.internal();
        return true;
    }
    public Operation expireChange(String draftId,String operationId,String failure) {
        Draft draft=lockDraft(draftId);Operation op=operation(draftId,operationId,true);
        if(!op.type().equals("CHANGE")||op.terminal()||databaseTime().isBefore(op.expires()))return op;
        return failLocked(draft,op,failure);
    }
    /** 호출자가 해당 worker/시도의 종료를 확인한 경우에만 사용한다. 시작 때 전체 RUNNING을 실패로 바꾸지 않는다. */
    public Operation failStoppedChange(String draftId,String operationId,String stoppedToken,String failure) {
        Draft draft=lockDraft(draftId);Operation op=operation(draftId,operationId,true);
        if(!op.type().equals("CHANGE")||!op.status().equals("RUNNING")||stoppedToken==null||!stoppedToken.equals(op.token()))return op;
        return failLocked(draft,op,failure);
    }
    private Operation failLocked(Draft draft,Operation op,String failure) {
        if(!op.id().equals(draft.activeOperation()))throw InputDraftException.internal();
        jdbc.update("UPDATE tmd_mdl_inptdrftprcsl SET prcs_stat_cd='FAILED',fail_json=?,end_dtm=CURRENT_TIMESTAMP(6),mod_dtm=CURRENT_TIMESTAMP WHERE drft_id=? AND prcs_id=? AND prcs_stat_cd IN ('QUEUED','RUNNING')",failure,draft.id(),op.id());
        int changed=jdbc.update("UPDATE tmd_mdl_inptdrftm SET actv_prcs_id=NULL,mod_usr_logn_id=?,mod_dtm=CURRENT_TIMESTAMP WHERE drft_id=? AND actv_prcs_id=?",op.actor(),draft.id(),op.id());
        if(changed!=1)throw InputDraftException.internal();
        return operation(draft.id(),op.id(),false);
    }
    public Map<String,Long> fixedFiles(String draft) {
        Map<String,Long> files=new LinkedHashMap<>();
        jdbc.query("SELECT mdl_file_cd,file_no FROM tmd_mdl_inptdrftfiled WHERE drft_id=? AND del_yn='N'",rs->{files.put(rs.getString(1),rs.getLong(2));},draft);
        return files;
    }
    public List<RowRef> rows(String draft,long table) {
        return jdbc.query("""
            SELECT m.inpt_tbl_id,h.row_no,m.row_id FROM tmd_mdl_inptdrftrowm m
            JOIN tmd_mdl_inptdrftrowh h ON h.row_id=m.row_id AND h.drft_id=m.drft_id AND h.inpt_tbl_id=m.inpt_tbl_id
            WHERE m.drft_id=? AND m.inpt_tbl_id=? AND m.del_yn='N' AND h.del_yn='N' AND h.end_rvsn_no IS NULL AND h.row_no IS NOT NULL ORDER BY h.row_no
            """,(rs,n)->new RowRef(rs.getLong(1),rs.getInt(2),rs.getString(3)),draft,table);
    }
    public Draft create(int company,int user,long caseId,Catalog catalog,String version,List<FileRef> files,List<RowRef> rows,String actor) {
        String id=UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO tmd_mdl_inptdrftm(drft_id,cust_cpny_no,usr_no,org_case_id,tmplt_id,org_rvsn_val,drft_stat_cd,rvsn_no,crt_usr_logn_id,mod_usr_logn_id)
            VALUES(?,?,?,?,?,?,'DRAFT',0,?,?)
            """,id,company,user,caseId,catalog.templateId(),version,actor,actor);
        for(FileRef file:files) jdbc.update("INSERT INTO tmd_mdl_inptdrftfiled(drft_id,mdl_file_cd,file_no,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,?,?,?)",id,file.role(),file.fileNo(),actor,actor);
        jdbc.update("INSERT INTO tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no,json_rvsn_no,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,0,2,?,?)",id,actor,actor);
        insertOriginalRows(id, rows, actor, "IDENTITY",
            "INSERT INTO tmd_mdl_inptdrftrowm(row_id,drft_id,inpt_tbl_id,org_row_no,crt_usr_logn_id,mod_usr_logn_id) VALUES",
            "(?,?,?,?,?,?)");
        insertOriginalRows(id, rows, actor, "POSITION",
            "INSERT INTO tmd_mdl_inptdrftrowh(row_id,bgn_rvsn_no,drft_id,inpt_tbl_id,row_no,crt_usr_logn_id,mod_usr_logn_id) VALUES",
            "(?,0,?,?,?,?,?)");
        return owned(id,company,user);
    }

    /** 두 고정 INSERT만 사용한다. 행 값은 모두 바인딩하고 SQL 전송당 행 수를 제한한다. */
    private void insertOriginalRows(String draft, List<RowRef> rows, String actor, String phase, String statement, String tuple) {
        for (int offset = 0; offset < rows.size(); offset += ORIGINAL_ROW_INSERT_SIZE) {
            List<RowRef> chunk = rows.subList(offset, Math.min(offset + ORIGINAL_ROW_INSERT_SIZE, rows.size()));
            String sql = statement + String.join(",", Collections.nCopies(chunk.size(), tuple));
            int rowsBefore = offset;
            long started = System.nanoTime();
            jdbc.update(sql, ps -> {
                int parameter = 1;
                for (RowRef row : chunk) {
                    ps.setString(parameter++, row.id());
                    ps.setString(parameter++, draft);
                    ps.setLong(parameter++, row.table());
                    ps.setInt(parameter++, row.row());
                    ps.setString(parameter++, actor);
                    ps.setString(parameter++, actor);
                }
                if (rowsBefore == 0) {
                    log.info("Input Draft row insert started phase={} connectionId={} totalRows={} chunkRows={}",
                        phase, mysqlConnectionId(ps), rows.size(), chunk.size());
                } else if (log.isDebugEnabled()) {
                    log.debug("Input Draft row insert sending phase={} connectionId={} rowsBefore={} chunkRows={}",
                        phase, mysqlConnectionId(ps), rowsBefore, chunk.size());
                }
            });
            log.info("Input Draft row insert progress phase={} completedRows={} totalRows={} chunkMs={}",
                phase, offset + chunk.size(), rows.size(), (System.nanoTime() - started) / 1_000_000);
        }
    }

    /** 현재 statement의 물리 연결 번호만 읽는다. 진단용 unwrap 실패는 저장 흐름에 영향을 주지 않는다. */
    private static Object mysqlConnectionId(java.sql.PreparedStatement statement) {
        try {
            var connection = statement.getConnection();
            if (connection.isWrapperFor(com.mysql.cj.jdbc.JdbcConnection.class)) {
                return connection.unwrap(com.mysql.cj.jdbc.JdbcConnection.class).getId();
            }
        } catch (SQLException | RuntimeException ignored) { /* 민감한 provider 메시지는 기록하지 않는다. */ }
        return "UNKNOWN";
    }
}
