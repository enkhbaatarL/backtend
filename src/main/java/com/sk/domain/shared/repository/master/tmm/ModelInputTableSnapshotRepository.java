package com.sk.domain.shared.repository.master.tmm;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 고정 원본 조회 사본. 생성 쓰기는 짧은 master transaction, READY 판독은 잠금 없이 수행한다. */
@Repository
@ConditionalOnProperty(name="mi.model-input.table-snapshot.mysql.enabled",havingValue="true")
public class ModelInputTableSnapshotRepository {
    public static final int VERSION=1;
    public static final int PART_LIMIT=8*1024*1024+2;
    public static final int BATCH_SIZE=4;
    private final JdbcTemplate sql;

    public ModelInputTableSnapshotRepository(@Qualifier("masterDataSource") DataSource source) {
        sql=new JdbcTemplate(source);sql.setQueryTimeout(10);
    }

    public record Job(String id,int company,int user,String companyCode,long caseId,String actor,String token,LocalDateTime expires) {}
    public record Stored(String id,String manifest,String hash) {}
    public record Part(int ordinal,byte[] bytes) {}

    /** Case 등록과 같은 transaction의 durable 생성 의도. 중복 등록은 기존 상태를 바꾸지 않는다. */
    public void enqueue(int company,int user,String companyCode,long caseId,String actor) {
        transaction();
        if(company<=0||user<=0||caseId<=0||companyCode==null||!companyCode.matches("[A-Za-z0-9_]{1,50}")
                ||actor==null||actor.isBlank()||actor.length()>50)throw new IllegalArgumentException("invalid snapshot owner");
        sql.update("""
            INSERT INTO tmd_mdl_inptsnapm(snap_id,cust_cpny_no,usr_no,cust_cpny_cd,org_case_id,base_ver_no,prcs_stat_cd,crt_usr_logn_id,mod_usr_logn_id)
            VALUES(?,?,?,?,?,1,'QUEUED',?,?) ON DUPLICATE KEY UPDATE snap_id=snap_id
            """,UUID.randomUUID().toString(),company,user,companyCode,caseId,actor,actor);
    }

    public Job claim() {
        transaction();
        sql.update("UPDATE tmd_mdl_inptsnapm SET prcs_stat_cd='FAILED',try_tokn=NULL,expr_dtm=NULL WHERE prcs_stat_cd='BUILDING' AND expr_dtm<=CURRENT_TIMESTAMP(6)");
        List<Job> jobs=sql.query("""
            SELECT snap_id,cust_cpny_no,usr_no,cust_cpny_cd,org_case_id,crt_usr_logn_id
            FROM tmd_mdl_inptsnapm WHERE prcs_stat_cd='QUEUED' AND base_ver_no=1 AND del_yn='N'
            ORDER BY crt_dtm,snap_id LIMIT 1
            """,(r,n)->new Job(r.getString(1),r.getInt(2),r.getInt(3),r.getString(4),r.getLong(5),r.getString(6),null,null));
        if(jobs.isEmpty())return null;
        Job job=jobs.get(0);String token=UUID.randomUUID().toString();
        int updated=sql.update("UPDATE tmd_mdl_inptsnapm SET prcs_stat_cd='BUILDING',try_tokn=?,expr_dtm=TIMESTAMPADD(SECOND,120,CURRENT_TIMESTAMP(6)) WHERE snap_id=? AND prcs_stat_cd='QUEUED'",
            token,job.id());
        if(updated!=1)return null;
        LocalDateTime expiry=sql.queryForObject("SELECT expr_dtm FROM tmd_mdl_inptsnapm WHERE snap_id=?",(r,n)->r.getObject(1,LocalDateTime.class),job.id());
        return new Job(job.id(),job.company(),job.user(),job.companyCode(),job.caseId(),job.actor(),token,expiry);
    }

    private void active(Job job) {
        transaction();
        List<String> ids=sql.queryForList("""
            SELECT snap_id FROM tmd_mdl_inptsnapm WHERE snap_id=? AND prcs_stat_cd='BUILDING'
              AND try_tokn=? AND expr_dtm>CURRENT_TIMESTAMP(6) FOR UPDATE
            """,String.class,job.id(),job.token());
        if(ids.size()!=1)throw new IllegalStateException("snapshot claim expired");
    }

    public void beginTable(Job job,long table) {
        active(job);
        sql.update("INSERT INTO tmd_mdl_inptsnapd(snap_id,inpt_tbl_id,obj_ref_json,cntt_hash_val) VALUES(?,?,'{}',?)",
            job.id(),table,"0".repeat(64));
    }

    public void part(Job job,long table,int ordinal,byte[] payload) {
        if(ordinal<0||ordinal>=1024||payload.length>PART_LIMIT)throw new IllegalArgumentException("snapshot part limit");
        active(job);
        sql.update("INSERT INTO tmd_mdl_inptsnapjsond(snap_id,inpt_tbl_id,json_ord,cntt_json) VALUES(?,?,?,?)",
            job.id(),table,ordinal,new String(payload,StandardCharsets.UTF_8));
    }

    public void manifest(Job job,long table,byte[] payload,String hash) {
        if(payload.length>1_048_576)throw new IllegalArgumentException("snapshot manifest limit");
        active(job);
        one(sql.update("UPDATE tmd_mdl_inptsnapd SET obj_ref_json=?,cntt_hash_val=? WHERE snap_id=? AND inpt_tbl_id=?",
            new String(payload,StandardCharsets.UTF_8),hash,job.id(),table));
    }

    public void ready(Job job,String sourceVersion) {
        active(job);
        one(sql.update("UPDATE tmd_mdl_inptsnapm SET prcs_stat_cd='READY',org_rvsn_val=?,try_tokn=NULL,expr_dtm=NULL WHERE snap_id=? AND try_tokn=? AND expr_dtm>CURRENT_TIMESTAMP(6)",
            sourceVersion,job.id(),job.token()));
    }

    public void failed(Job job) {
        transaction();
        // Failure never changes a READY header or another token, and never deletes business/original data.
        sql.update("UPDATE tmd_mdl_inptsnapm SET prcs_stat_cd='FAILED',try_tokn=NULL,expr_dtm=NULL WHERE snap_id=? AND prcs_stat_cd='BUILDING' AND try_tokn=?",job.id(),job.token());
    }

    public Stored find(int company,long caseId,String sourceVersion,long table) {
        return sql.query("""
            SELECT m.snap_id,CASE WHEN OCTET_LENGTH(d.obj_ref_json)<=1048576 THEN d.obj_ref_json ELSE NULL END,d.cntt_hash_val
            FROM tmd_mdl_inptsnapm m JOIN tmd_mdl_inptsnapd d ON d.snap_id=m.snap_id
            WHERE m.cust_cpny_no=? AND m.org_case_id=? AND m.base_ver_no=1 AND m.prcs_stat_cd='READY'
              AND m.org_rvsn_val=? AND m.del_yn='N' AND d.inpt_tbl_id=? AND d.del_yn='N'
            """,(r,n)->new Stored(r.getString(1),r.getString(2),r.getString(3)),company,caseId,sourceVersion,table)
            .stream().findFirst().orElse(null);
    }

    /** At most four bounded parts; payloads above the limit are not transferred to the client. */
    public List<Part> parts(String id,long table,int first) {
        return sql.query("""
            SELECT json_ord,CASE WHEN OCTET_LENGTH(cntt_json)<=8388610 THEN cntt_json ELSE NULL END
            FROM tmd_mdl_inptsnapjsond WHERE snap_id=? AND inpt_tbl_id=? AND json_ord>=? AND json_ord<? AND del_yn='N' ORDER BY json_ord
            """,(r,n)->{
                String payload=r.getString(2);
                if(payload==null)throw new IllegalStateException("snapshot part limit");
                return new Part(r.getInt(1),payload.getBytes(StandardCharsets.UTF_8));
            },id,table,first,first+BATCH_SIZE);
    }

    private static void transaction() {
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("master transaction required");
    }
    private static void one(int count) {if(count!=1)throw new IllegalStateException("snapshot write rejected");}
}
