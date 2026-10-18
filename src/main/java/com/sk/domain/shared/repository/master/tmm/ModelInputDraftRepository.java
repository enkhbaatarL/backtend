package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.mi.service.fundamentalmodel.input.InputDraftException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** master transaction에 참여하는 명시적 SQL. 원본 projection과 초기 행 대응 bulk 저장에 사용한다. */
@Repository
public class ModelInputDraftRepository {
    private final JdbcTemplate jdbc;
    @Value("${mi.model-input.model-code:}")
    private String modelCode;
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
        if(modelCode==null||modelCode.isBlank()) throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        List<long[]> root=jdbc.query("""
            SELECT t.tmplt_id,t.tmplt_ver_no FROM tmd_mdl_scnrcasetmpltd c
            JOIN tmd_mdl_inpttmpltm t ON t.tmplt_id=c.tmplt_id
            WHERE c.case_id=? AND c.del_yn='N' AND t.del_yn='N' AND t.mdl_spt_cd=? AND t.tmplt_stat_cd IN ('ACTIVE','DEPRECATED')
            """,(rs,n)->new long[]{rs.getLong(1),rs.getInt(2)},caseId,modelCode);
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
    public Draft owned(String id,int company,int user) {
        return jdbc.query("SELECT * FROM tmd_mdl_inptdrftm WHERE drft_id=? AND cust_cpny_no=? AND usr_no=? AND del_yn='N'",
            this::draft,id,company,user).stream().findFirst().orElseThrow(InputDraftException::missing);
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
