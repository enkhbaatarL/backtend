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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** master transaction에 참여하는 명시적 SQL. 원본 projection과 초기 행 대응 bulk 저장에 사용한다. */
@Repository
public class ModelInputDraftRepository {
    private final JdbcTemplate jdbc;
    public ModelInputDraftRepository(@Qualifier("masterDataSource") DataSource master) { jdbc=new JdbcTemplate(master); }
    public record FileRef(String role,long fileNo,String name,String location) {}
    public record Scope(int startRow,int endRow,int startColumn,int endColumn) {
        public boolean contains(int row,int col) { return row>=startRow&&row<=endRow&&col>=startColumn&&col<=endColumn; }
    }
    public record TableRef(long id,String role,String sheet,String name,String description,int startRow,int endRow,
            int startColumn,int endColumn,int headerStart,int headerEnd,int dataStart,boolean rowAdd,boolean columnAdd,
            String definition,List<Scope> scopes) {
        public String key() { return role+"/"+sheet+"/"+name; }
    }
    public record Catalog(long templateId,int version,List<String> roles,List<TableRef> tables) {}
    public record Draft(String id,int company,int user,long caseId,long templateId,String sourceVersion,String status,
            long revision,String activeOperation,Long finalizedCase) {}
    public record RowRef(long table,int row,String id) {}
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
        long template=root.get(0)[0];
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
        return new Catalog(template,(int)root.get(0)[1],List.copyOf(roles),List.copyOf(tables));
    }
    private Draft draft(ResultSet rs,int ignored)throws SQLException {
        return new Draft(rs.getString("drft_id"),rs.getInt("cust_cpny_no"),rs.getInt("usr_no"),rs.getLong("org_case_id"),
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
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftm WHERE drft_id=? AND cust_cpny_no=? AND usr_no=? AND del_yn='N' FOR UPDATE",
            this::draft,id,company,user).stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    private Draft lockDraft(String id) {
        requireTransaction();
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftm WHERE drft_id=? AND del_yn='N' FOR UPDATE",this::draft,id)
            .stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    public Draft lockForRecovery(String id){return lockDraft(id);}
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
    public Operation enqueueChange(Draft draft,String requestId,long baseRevision,String request,String role,String actor) {
        Draft current=lockDraft(draft.id());
        if(!current.status().equals("DRAFT"))throw InputDraftException.conflict("DRAFT_NOT_EDITABLE",current.revision());
        if(current.activeOperation()!=null)throw InputDraftException.conflict("DRAFT_BUSY",current.revision());
        if(current.revision()!=baseRevision)throw InputDraftException.conflict("REVISION_CONFLICT",current.revision());
        // 감사 시간은 초 단위다. 같은 값을 기한의 기준으로 써 DB 반올림/CHECK 불일치를 막는다.
        LocalDateTime now=databaseTime().withNano(0);String id=UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO tmd_mdl_inptdrftprcsl(prcs_id,drft_id,req_id,prcs_type_cd,usr_role_cd,base_rvsn_no,prcs_stat_cd,json_rvsn_no,req_json,expr_dtm,crt_dtm,mod_dtm,crt_usr_logn_id,mod_usr_logn_id)
            VALUES(?,?,?,'CHANGE',?,?,'QUEUED',1,?,?,?,?,?,?)
            """,id,current.id(),requestId,role,baseRevision,request,now.plusSeconds(120),now,now,actor,actor);
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
    public Operation claimChange(String draftId,String operationId) {
        return claimChange(draftId,operationId,UUID.randomUUID().toString());
    }
    /** marker를 먼저 fsync한 서버 토큰만 전달한다. HTTP 입력으로 받지 않는다. */
    public Operation claimChange(String draftId,String operationId,String token) {
        if(token==null||!UUID.fromString(token).toString().equals(token))throw InputDraftException.internal();
        Draft draft=lockDraft(draftId);Operation op=operation(draftId,operationId,true);
        if(op.schemaVersion()!=1||!op.type().equals("CHANGE"))throw InputDraftException.internal();
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
        return op.schemaVersion()==1&&op.type().equals("CHANGE")&&op.status().equals("RUNNING")&&token!=null&&token.equals(op.token())
            &&draft.status().equals("DRAFT")&&operationId.equals(draft.activeOperation())&&draft.revision()==op.baseRevision()
            &&databaseTime().isBefore(op.expires())?op:null;
    }
    public record Revision(long number,Long base,String operation,int schema,String delta,
        String type,String status,int operationSchema,Long operationBase,String request,String result) {}
    public record RowIdentity(long table,String id,Integer original,String operation,String client) {}
    public record RowPosition(long table,String id,long begin,Long end,Integer row) {}
    public record RowWrite(long table,String id,String client,Integer row,boolean added) {}
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
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftrowm WHERE drft_id=? ORDER BY inpt_tbl_id,row_id",(rs,n)->{
            if(!"N".equals(rs.getString("del_yn")))throw InputDraftException.internal();
            return new RowIdentity(rs.getLong("inpt_tbl_id"),rs.getString("row_id"),rs.getObject("org_row_no",Integer.class),rs.getString("prcs_id"),rs.getString("clnt_row_id"));
        },draftId);
    }
    public List<RowPosition> rowPositions(String draftId) {
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftrowh WHERE drft_id=? ORDER BY row_id,bgn_rvsn_no",(rs,n)->{
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
            VALUES(?,?,1,?,?,?,?,?)
            """,draftId,next,op.baseRevision(),op.id(),delta,op.actor(),op.actor());
        for(RowWrite row:rows)if(row.added())jdbc.update("""
            INSERT INTO tmd_mdl_inptdrftrowm(row_id,drft_id,inpt_tbl_id,prcs_id,clnt_row_id,crt_usr_logn_id,mod_usr_logn_id)
            VALUES(?,?,?,?,?,?,?)
            """,row.id(),draftId,row.table(),op.id(),row.client(),op.actor(),op.actor());
        // 위치 교환/이동은 모든 이전 current를 닫은 뒤 새 위치를 넣어 generated UK를 지킨다.
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
        jdbc.update("INSERT INTO tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no,json_rvsn_no,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,0,1,?,?)",id,actor,actor);
        jdbc.batchUpdate("INSERT INTO tmd_mdl_inptdrftrowm(row_id,drft_id,inpt_tbl_id,org_row_no,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,?,?,?,?)",rows,500,
            (ps,row)->{ps.setString(1,row.id());ps.setString(2,id);ps.setLong(3,row.table());ps.setInt(4,row.row());ps.setString(5,actor);ps.setString(6,actor);});
        jdbc.batchUpdate("INSERT INTO tmd_mdl_inptdrftrowh(row_id,bgn_rvsn_no,drft_id,inpt_tbl_id,row_no,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,0,?,?,?,?,?)",rows,500,
            (ps,row)->{ps.setString(1,row.id());ps.setString(2,id);ps.setLong(3,row.table());ps.setInt(4,row.row());ps.setString(5,actor);ps.setString(6,actor);});
        return owned(id,company,user);
    }
}
