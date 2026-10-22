package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 직접 업로드도 공통 operation/revision을 사용한다. 모든 변경은 owner-slot → Draft → operation 순서다. */
@Repository
@Slf4j
public class InputUploadRepository {
    private static final int UPLOAD_DEADLINE_SECONDS = 600;
    private static final int ROW_INSERT_SIZE = 500;
    private final JdbcTemplate sql;
    private final ModelInputDraftRepository common;

    public InputUploadRepository(@Qualifier("masterDataSource") DataSource source, ModelInputDraftRepository common) {
        sql = new JdbcTemplate(source);
        this.common = common;
    }

    record Slot(String operation, String draft) {}
    record Upload(String id, int company, int user, String reference, String name, String extension,
            String mime, Long size, String hash, String operation, String kind, Long template,
            String draft, Long revision, String status) {
        FileRef file(String location) { return new FileRef(kind, null, name, location, revision == null ? 0 : revision, hash, size); }
    }

    private Upload upload(ResultSet r, int ignored) throws SQLException {
        return new Upload(r.getString("upld_file_id"),r.getInt("cust_cpny_no"),r.getInt("usr_no"),
            r.getString("strg_ref_json"),r.getString("org_file_nm"),r.getString("filex_nm"),r.getString("mime_type_nm"),
            r.getObject("file_size",Long.class),r.getString("cntt_hash_val"),r.getString("prcs_id"),r.getString("mdl_file_cd"),
            r.getObject("tmplt_id",Long.class),r.getString("drft_id"),r.getObject("file_rvsn_no",Long.class),r.getString("prcs_stat_cd"));
    }
    Slot lockSlot(int company, int user, String actor) {
        requireTransaction();
        // An existing row is never replaced: another tab may already own it.
        sql.update("INSERT INTO tmd_mdl_inptupldactvm(cust_cpny_no,usr_no,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE cust_cpny_no=cust_cpny_no",company,user,actor,actor);
        return sql.queryForObject("SELECT prcs_id,drft_id FROM tmd_mdl_inptupldactvm WHERE cust_cpny_no=? AND usr_no=? FOR UPDATE",
            (r,n)->new Slot(r.getString(1),r.getString(2)),company,user);
    }
    void prepare(String id,int company,int user,String actor,String ref,String name,String extension,String mime) {
        sql.update("INSERT INTO tmd_mdl_inptupldfiled(upld_file_id,cust_cpny_no,usr_no,strg_ref_json,org_file_nm,filex_nm,mime_type_nm,prcs_stat_cd,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,?,?,?,?,?,'PREPARED',?,?)",
            id,company,user,ref,name,extension,mime,actor,actor);
    }
    void received(String id,long size,String hash) {
        one(sql.update("UPDATE tmd_mdl_inptupldfiled SET file_size=?,cntt_hash_val=?,prcs_stat_cd='RECEIVED' WHERE upld_file_id=? AND prcs_stat_cd='PREPARED'",size,hash,id));
    }
    Upload file(String id) {
        return sql.query("SELECT * FROM tmd_mdl_inptupldfiled WHERE upld_file_id=?",this::upload,id)
            .stream().findFirst().orElseThrow(InputDraftException::internal);
    }
    List<Upload> operationFiles(String operation) {
        List<Upload> files=sql.query("SELECT * FROM tmd_mdl_inptupldfiled WHERE prcs_id=? ORDER BY upld_file_id",this::upload,operation);
        if(files.isEmpty()||files.size()>4)throw InputDraftException.internal();
        return files;
    }
    List<Upload> files(String draft) {
        return sql.query("SELECT u.* FROM tmd_mdl_inptdrftfiled f JOIN tmd_mdl_inptupldfiled u ON u.upld_file_id=f.upld_file_id AND u.drft_id=f.drft_id WHERE f.drft_id=? AND f.del_yn='N' AND u.prcs_stat_cd='COMMITTED' ORDER BY f.mdl_file_cd",this::upload,draft);
    }
    private Operation mapOperation(ResultSet r,int ignored)throws SQLException {
        return new Operation(r.getString("prcs_id"),r.getString("drft_id"),r.getString("req_id"),r.getString("prcs_type_cd"),
            r.getString("usr_role_cd"),r.getLong("base_rvsn_no"),r.getString("prcs_stat_cd"),r.getInt("json_rvsn_no"),r.getString("req_json"),
            r.getString("try_tokn"),r.getTimestamp("expr_dtm").toLocalDateTime(),r.getString("rslt_json"),r.getString("fail_json"),r.getString("crt_usr_logn_id"));
    }
    Operation ownedOperation(String id,int company,int user) {
        return sql.query("SELECT * FROM tmd_mdl_inptdrftprcsl WHERE prcs_id=? AND cust_cpny_no=? AND usr_no=? AND prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE')",this::mapOperation,id,company,user)
            .stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    Operation operation(String id) {
        return sql.query("SELECT * FROM tmd_mdl_inptdrftprcsl WHERE prcs_id=? AND prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE')",this::mapOperation,id)
            .stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    WorkerOwner owner(Operation op) {
        return sql.query("""
            SELECT p.cust_cpny_no,p.usr_no,c.cust_cpny_cd,u.usr_logn_id
            FROM tmd_mdl_inptdrftprcsl p JOIN tmd_usrm u ON u.usr_no=p.usr_no
            JOIN tmd_custcpnym c ON c.cust_cpny_no=p.cust_cpny_no
            WHERE p.prcs_id=? AND u.del_yn='N' AND c.del_yn='N'
              AND u.usr_logn_id=? AND u.usr_role_cd=? AND u.usr_role_cd IN ('USER','ADMIN')
              AND ((u.usr_role_cd='USER' AND u.cust_cpny_no=p.cust_cpny_no AND c.cust_cpny_cd='SKGAS')
                OR (u.usr_role_cd='ADMIN' AND u.otcmp_svc_accs_psbl_yn='Y'))
            """,(r,n)->new WorkerOwner(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4)),op.id(),op.actor(),op.role())
            .stream().findFirst().orElseThrow(InputDraftException::missing);
    }
    Operation previous(int company,int user,String draft,String request) {
        if(draft!=null)return common.request(draft,request);
        return sql.query("SELECT * FROM tmd_mdl_inptdrftprcsl WHERE cust_cpny_no=? AND usr_no=? AND crt_req_id=?",this::mapOperation,company,user,request)
            .stream().findFirst().orElse(null);
    }
    Operation accept(List<Upload> files,String draft,String request,Long revision,String role,String actor,String payload) {
        requireTransaction();Upload file=files.get(0);String id=UUID.randomUUID().toString();var now=common.databaseTime().withNano(0);
        sql.update("INSERT INTO tmd_mdl_inptdrftprcsl(prcs_id,cust_cpny_no,usr_no,drft_id,req_id,prcs_type_cd,usr_role_cd,base_rvsn_no,prcs_stat_cd,json_rvsn_no,req_json,expr_dtm,crt_dtm,mod_dtm,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,?,?,?,?,?,?,'QUEUED',2,?,?,?,?,?,?)",
            id,file.company,file.user,draft,request,draft==null?"UPLOAD_CREATE":"UPLOAD_FILE",role,revision,payload,now.plusSeconds(UPLOAD_DEADLINE_SECONDS),now,now,actor,actor);
        for(Upload incoming:files)one(sql.update("UPDATE tmd_mdl_inptupldfiled SET prcs_id=? WHERE upld_file_id=? AND prcs_id IS NULL AND prcs_stat_cd='RECEIVED'",id,incoming.id));
        if(draft==null)one(sql.update("UPDATE tmd_mdl_inptupldactvm SET prcs_id=? WHERE cust_cpny_no=? AND usr_no=? AND prcs_id IS NULL AND drft_id IS NULL",id,file.company,file.user));
        else one(sql.update("UPDATE tmd_mdl_inptdrftm SET actv_prcs_id=? WHERE drft_id=? AND actv_prcs_id IS NULL AND rvsn_no=? AND drft_stat_cd='DRAFT'",id,draft,revision));
        return operation(id);
    }
    List<Operation> pending() { return sql.query("SELECT * FROM tmd_mdl_inptdrftprcsl WHERE prcs_type_cd IN ('UPLOAD_CREATE','UPLOAD_FILE') AND prcs_stat_cd IN ('QUEUED','RUNNING') ORDER BY expr_dtm,prcs_id LIMIT 100",this::mapOperation); }
    Operation lock(Operation observed) {
        Upload u=operationFiles(observed.id()).get(0);lockSlot(u.company,u.user,observed.actor());
        if(observed.draftId()!=null)common.lockForRecovery(observed.draftId());
        return sql.queryForObject("SELECT * FROM tmd_mdl_inptdrftprcsl WHERE prcs_id=? FOR UPDATE",this::mapOperation,observed.id());
    }
    Operation claim(Operation observed,String token) {
        var op=lock(observed);
        if(!op.status().equals("QUEUED")||!common.databaseTime().isBefore(op.expires()))return null;
        one(sql.update("UPDATE tmd_mdl_inptdrftprcsl SET prcs_stat_cd='RUNNING',try_tokn=?,bgn_dtm=CURRENT_TIMESTAMP(6) WHERE prcs_id=? AND prcs_stat_cd='QUEUED'",token,op.id()));
        return operation(op.id());
    }
    Catalog template(Long id,String modelCode) {
        List<long[]> rows= id==null ? sql.query("SELECT tmplt_id,tmplt_ver_no FROM tmd_mdl_inpttmpltm WHERE mdl_spt_cd=? AND tmplt_stat_cd='ACTIVE' AND del_yn='N'",(r,n)->new long[]{r.getLong(1),r.getLong(2)},modelCode)
            :sql.query("SELECT tmplt_id,tmplt_ver_no FROM tmd_mdl_inpttmpltm WHERE tmplt_id=? AND tmplt_stat_cd IN ('ACTIVE','DEPRECATED') AND del_yn='N'",(r,n)->new long[]{r.getLong(1),r.getLong(2)},id);
        if(rows.size()!=1)throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        return common.catalogTemplate(rows.get(0)[0],(int)rows.get(0)[1]);
    }
    void validation(Upload u,String kind,long template) {
        one(sql.update("UPDATE tmd_mdl_inptupldfiled SET mdl_file_cd=?,tmplt_id=? WHERE upld_file_id=? AND prcs_stat_cd='RECEIVED' AND tmplt_id IS NULL",kind,template,u.id));
    }
    void createDraft(String draft,Upload u,Catalog catalog,String actor) {
        sql.update("INSERT INTO tmd_mdl_inptdrftm(drft_id,cust_cpny_no,usr_no,src_spt_cd,org_case_id,org_rvsn_val,tmplt_id,drft_stat_cd,rvsn_no,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,?,'UPLOAD',NULL,NULL,?,'DRAFT',0,?,?)",draft,u.company,u.user,catalog.templateId(),actor,actor);
        sql.update("INSERT INTO tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no,json_rvsn_no,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,0,2,?,?)",draft,actor,actor);
    }
    void complete(Operation op,String draft,long revision,String result,String delta,List<RowRef> rows,Catalog catalog) {
        List<Upload> incoming=operationFiles(op.id());Upload u=incoming.get(0);
        one(sql.update("UPDATE tmd_mdl_inptdrftprcsl SET drft_id=?,prcs_stat_cd='SUCCEEDED',rslt_json=?,end_dtm=CURRENT_TIMESTAMP(6) WHERE prcs_id=? AND prcs_stat_cd='RUNNING' AND try_tokn=? AND expr_dtm>CURRENT_TIMESTAMP(6)",draft,result,op.id(),op.token()));
        if(revision>0)sql.update("INSERT INTO tmd_mdl_inptdrftrvsnl(drft_id,rvsn_no,base_rvsn_no,prcs_id,json_rvsn_no,dlta_json,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,?,?,2,?,?,?)",draft,revision,revision-1,op.id(),delta,op.actor(),op.actor());
        for(Upload file:incoming) {
            one(sql.update("UPDATE tmd_mdl_inptupldfiled SET drft_id=?,file_rvsn_no=?,prcs_stat_cd='COMMITTED' WHERE upld_file_id=? AND prcs_stat_cd='RECEIVED'",draft,revision,file.id));
            sql.update("INSERT INTO tmd_mdl_inptdrftfiled(drft_id,mdl_file_cd,file_no,upld_file_id,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,NULL,?,?,?) ON DUPLICATE KEY UPDATE upld_file_id=VALUES(upld_file_id),mod_usr_logn_id=VALUES(mod_usr_logn_id)",draft,file.kind,file.id,op.actor(),op.actor());
        }
        var kinds=incoming.stream().map(Upload::kind).collect(java.util.stream.Collectors.toSet());
        var affected=catalog.tables().stream().filter(t->kinds.contains(t.role())).map(TableRef::id).collect(java.util.stream.Collectors.toSet());
        List<RowPosition> active=common.physicalRowPositions(draft).stream().filter(p->affected.contains(p.table())&&p.end()==null&&p.row()!=null).toList();
        sql.batchUpdate("UPDATE tmd_mdl_inptdrftrowh SET end_rvsn_no=? WHERE row_id=? AND end_rvsn_no IS NULL",active,500,
            (s,p)->{s.setLong(1,revision);s.setString(2,p.id());});
        sql.batchUpdate("INSERT INTO tmd_mdl_inptdrftrowh(row_id,drft_id,inpt_tbl_id,bgn_rvsn_no,row_no,crt_usr_logn_id,mod_usr_logn_id) VALUES(?,?,?,?,NULL,?,?)",active,500,
            (s,p)->{s.setString(1,p.id());s.setString(2,draft);s.setLong(3,p.table());s.setLong(4,revision);s.setString(5,op.actor());s.setString(6,op.actor());});
        insertOriginalRows(draft,revision,op.actor(),rows);
        if(revision>0)one(sql.update("UPDATE tmd_mdl_inptdrftm SET rvsn_no=?,actv_prcs_id=NULL WHERE drft_id=? AND actv_prcs_id=? AND rvsn_no=?",revision,draft,op.id(),revision-1));
        else one(sql.update("UPDATE tmd_mdl_inptupldactvm SET prcs_id=NULL,drft_id=? WHERE cust_cpny_no=? AND usr_no=? AND prcs_id=?",draft,u.company,u.user,op.id()));
        if(sql.update("UPDATE tmd_mdl_inptdrftprcsl SET end_dtm=CURRENT_TIMESTAMP(6) WHERE prcs_id=? AND prcs_stat_cd='SUCCEEDED' AND try_tokn=? AND expr_dtm>CURRENT_TIMESTAMP(6)",op.id(),op.token())!=1)
            throw InputDraftException.conflict("TIMEOUT",revision==0?null:revision-1);
    }
    /** 한 SQL에 500행만 바인딩하여 대량 업로드의 행별 DB 왕복을 피한다. */
    private void insertOriginalRows(String draft,long revision,String actor,List<RowRef> rows) {
        long started=System.nanoTime();
        log.info("Input upload row insert started phase=IDENTITY totalRows={}",rows.size());
        for(int offset=0;offset<rows.size();offset+=ROW_INSERT_SIZE) {
            InputTempFiles.checkpoint();
            List<RowRef> chunk=rows.subList(offset,Math.min(offset+ROW_INSERT_SIZE,rows.size()));
            String tuples=String.join(",",Collections.nCopies(chunk.size(),"(?,?,?,?,?,?,?)"));
            sql.update("INSERT INTO tmd_mdl_inptdrftrowm(row_id,drft_id,inpt_tbl_id,org_row_no,file_rvsn_no,crt_usr_logn_id,mod_usr_logn_id) VALUES"+tuples,statement->{
                int parameter=1;
                for(RowRef row:chunk){statement.setString(parameter++,row.id());statement.setString(parameter++,draft);statement.setLong(parameter++,row.table());statement.setInt(parameter++,row.row());statement.setLong(parameter++,revision);statement.setString(parameter++,actor);statement.setString(parameter++,actor);}
            });
            if((offset+chunk.size())%10000==0||offset+chunk.size()==rows.size())
                log.info("Input upload row insert progress phase=IDENTITY completedRows={} totalRows={} elapsedMs={}",offset+chunk.size(),rows.size(),(System.nanoTime()-started)/1_000_000);
        }
        started=System.nanoTime();
        log.info("Input upload row insert started phase=POSITION totalRows={}",rows.size());
        for(int offset=0;offset<rows.size();offset+=ROW_INSERT_SIZE) {
            InputTempFiles.checkpoint();
            List<RowRef> chunk=rows.subList(offset,Math.min(offset+ROW_INSERT_SIZE,rows.size()));
            String tuples=String.join(",",Collections.nCopies(chunk.size(),"(?,?,?,?,?,?,?)"));
            sql.update("INSERT INTO tmd_mdl_inptdrftrowh(row_id,drft_id,inpt_tbl_id,bgn_rvsn_no,row_no,crt_usr_logn_id,mod_usr_logn_id) VALUES"+tuples,statement->{
                int parameter=1;
                for(RowRef row:chunk){statement.setString(parameter++,row.id());statement.setString(parameter++,draft);statement.setLong(parameter++,row.table());statement.setLong(parameter++,revision);statement.setInt(parameter++,row.row());statement.setString(parameter++,actor);statement.setString(parameter++,actor);}
            });
            if((offset+chunk.size())%10000==0||offset+chunk.size()==rows.size())
                log.info("Input upload row insert progress phase=POSITION completedRows={} totalRows={} elapsedMs={}",offset+chunk.size(),rows.size(),(System.nanoTime()-started)/1_000_000);
        }
    }
    void fail(Operation observed,String failure) {
        Operation op=lock(observed);if(op.terminal())return;
        one(sql.update("UPDATE tmd_mdl_inptdrftprcsl SET prcs_stat_cd='FAILED',fail_json=?,end_dtm=CURRENT_TIMESTAMP(6) WHERE prcs_id=? AND prcs_stat_cd IN ('QUEUED','RUNNING')",failure,op.id()));
        if(op.draftId()==null)sql.update("UPDATE tmd_mdl_inptupldactvm SET prcs_id=NULL WHERE prcs_id=?",op.id());
        else one(sql.update("UPDATE tmd_mdl_inptdrftm SET actv_prcs_id=NULL WHERE drft_id=? AND actv_prcs_id=?",op.draftId(),op.id()));
        for(Upload file:operationFiles(op.id()))cleanup(file.id);
    }
    void cleanup(String file) { sql.update("UPDATE tmd_mdl_inptupldfiled SET prcs_stat_cd='CLEANUP_PENDING' WHERE upld_file_id=? AND prcs_stat_cd IN ('PREPARED','RECEIVED') AND (prcs_id IS NULL OR EXISTS(SELECT 1 FROM tmd_mdl_inptdrftprcsl p WHERE p.prcs_id=tmd_mdl_inptupldfiled.prcs_id AND p.prcs_stat_cd='FAILED'))",file); }
    void stoppedIntake(String file) {
        requireTransaction();
        sql.update("UPDATE tmd_mdl_inptupldfiled SET prcs_stat_cd='CLEANUP_PENDING' WHERE upld_file_id=? AND prcs_id IS NULL AND prcs_stat_cd IN ('PREPARED','RECEIVED')",file);
        // An admitted operation, or an absent row, is already accounted for. Never delete a successful baseline.
    }
    List<Upload> cleanupFiles() {return sql.query("SELECT * FROM tmd_mdl_inptupldfiled WHERE prcs_stat_cd='CLEANUP_PENDING' ORDER BY mod_dtm LIMIT 100",this::upload);}
    void cleaned(String file) {sql.update("UPDATE tmd_mdl_inptupldfiled SET prcs_stat_cd='CLEANED' WHERE upld_file_id=? AND prcs_stat_cd='CLEANUP_PENDING'",file);}
    void cleanupReference(String file,String ref){sql.update("UPDATE tmd_mdl_inptupldfiled SET strg_ref_json=? WHERE upld_file_id=? AND prcs_stat_cd='CLEANUP_PENDING'",ref,file);}
    String company(Upload file){return sql.queryForObject("SELECT cust_cpny_cd FROM tmd_custcpnym WHERE cust_cpny_no=?",String.class,file.company);}
    void discard(Draft draft) {
        one(sql.update("UPDATE tmd_mdl_inptdrftm SET drft_stat_cd='DISCARDED' WHERE drft_id=? AND drft_stat_cd='DRAFT' AND actv_prcs_id IS NULL",draft.id()));
        release(draft);
    }
    void release(Draft draft) {sql.update("UPDATE tmd_mdl_inptupldactvm SET drft_id=NULL WHERE cust_cpny_no=? AND usr_no=? AND drft_id=?",draft.company(),draft.user(),draft.id());}
    private static void one(int count) {if(count!=1)throw InputDraftException.internal();}
    private static void requireTransaction(){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("master transaction required");}
}
