package com.sk.domain.mi.service.fundamentalmodel.input;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** master queue의 소비자 수명. 행 누락/이전 ACTIVE는 자동 초기화하지 않는다. */
final class InputDraftRunnerRepository {
    record State(String execution, String status, String operation, String token) {}
    record Diagnostics(String database, Long lockOwnerConnectionId, String persistedExecution, String persistedStatus) {}
    private static final String ACTOR = "INPUT_WEB_RUNNER";
    private final JdbcTemplate sql;
    private final TransactionTemplate transaction;

    InputDraftRunnerRepository(DataSource master, PlatformTransactionManager manager) {
        sql = new JdbcTemplate(master);
        sql.setQueryTimeout(5);
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setTimeout(10);
    }

    State read() { return read(false); }

    /** 로그용 관측값이다. 잠금 소유 확인이나 이전 작업 종료 판단에 사용하지 않는다. */
    Diagnostics diagnostics() {
        return sql.query("""
            SELECT DATABASE(),IS_USED_LOCK(CONCAT('mi-input-draft:',DATABASE())),s.exec_id,s.exec_stat_cd
            FROM (SELECT 1 AS probe_value) probe
            LEFT JOIN tmd_mdl_inptexecm s ON s.exec_no=1 AND s.del_yn='N'
            """, (rs, n) -> new Diagnostics(rs.getString(1), rs.getObject(2, Long.class),
                rs.getString(3), rs.getString(4))).get(0);
    }

    private State read(boolean lock) {
        return sql.query("SELECT exec_id,exec_stat_cd,prcs_id,try_tokn FROM tmd_mdl_inptexecm WHERE exec_no=1 AND del_yn='N'"
                + (lock ? " FOR UPDATE" : ""),
                (rs, n) -> new State(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)))
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("runner state requires manual initialization"));
    }

    String activate() {
        return tx(() -> {
            State previous = read(true);
            if (!previous.status().equals("STOPPED")) return null;
            String next = UUID.randomUUID().toString();
            one(sql.update("""
                UPDATE tmd_mdl_inptexecm SET exec_id=?,exec_stat_cd='ACTIVE',bgn_dtm=CURRENT_TIMESTAMP(6),
                    end_dtm=NULL,mod_usr_logn_id=?,mod_dtm=CURRENT_TIMESTAMP WHERE exec_no=1 AND exec_stat_cd='STOPPED'
                """, next, ACTOR));
            return next;
        });
    }

    String approveRecovery(State expected, String actor) {
        InputDraftFinalizationService.uuid(expected.execution());
        if (actor == null || !actor.matches("[a-zA-Z0-9._@-]{1,50}")) throw new IllegalArgumentException("operator identity required");
        if ((expected.operation() == null) != (expected.token() == null)) throw new IllegalArgumentException("operation/token pair required");
        if (expected.operation() != null) {
            InputDraftFinalizationService.uuid(expected.operation());
            InputDraftFinalizationService.uuid(expected.token());
        }
        return tx(() -> {
            State current = read(true);
            if (current.status().equals("STOPPED") || !Objects.equals(expected.execution(), current.execution())
                    || !Objects.equals(expected.operation(), current.operation()) || !Objects.equals(expected.token(), current.token())) {
                throw new IllegalStateException("recovery target changed; approval rejected");
            }
            String next = UUID.randomUUID().toString();
            one(sql.update("""
                UPDATE tmd_mdl_inptexecm SET exec_id=?,exec_stat_cd='RECOVERING',bgn_dtm=CURRENT_TIMESTAMP(6),end_dtm=NULL,
                    aprv_exec_id=?,aprv_usr_logn_id=?,aprv_dtm=CURRENT_TIMESTAMP(6),mod_usr_logn_id=?,mod_dtm=CURRENT_TIMESTAMP
                WHERE exec_no=1 AND exec_id=?
                """, next, current.execution(), actor, actor, current.execution()));
            return next;
        });
    }

    void requireOwner(String execution) { tx(() -> { owned(execution); return null; }); }

    void bind(String execution, String operation, String token) {
        InputDraftFinalizationService.uuid(operation);
        InputDraftFinalizationService.uuid(token);
        tx(() -> {
            State current = owned(execution);
            if (current.operation() != null) throw new IllegalStateException("previous runner work remains");
            one(sql.update("UPDATE tmd_mdl_inptexecm SET prcs_id=?,try_tokn=?,mod_dtm=CURRENT_TIMESTAMP WHERE exec_no=1 AND exec_id=?",
                    operation, token, execution));
            return null;
        });
    }

    void releaseWork(String execution, String operation, String token) {
        tx(() -> {
            State current = owned(execution);
            if (!Objects.equals(operation, current.operation()) || !Objects.equals(token, current.token())) {
                throw new IllegalStateException("runner work changed");
            }
            one(sql.update("UPDATE tmd_mdl_inptexecm SET prcs_id=NULL,try_tokn=NULL,mod_dtm=CURRENT_TIMESTAMP WHERE exec_no=1 AND exec_id=?", execution));
            return null;
        });
    }

    void stopped(String execution) {
        tx(() -> {
            State current = owned(execution);
            if (current.operation() != null) throw new IllegalStateException("runner work not resolved");
            one(sql.update("UPDATE tmd_mdl_inptexecm SET exec_stat_cd='STOPPED',end_dtm=CURRENT_TIMESTAMP(6),mod_dtm=CURRENT_TIMESTAMP WHERE exec_no=1 AND exec_id=?", execution));
            return null;
        });
    }

    private State owned(String execution) {
        State state = read(true);
        if (execution == null || !execution.equals(state.execution()) || state.status().equals("STOPPED")) {
            throw new IllegalStateException("runner ownership lost");
        }
        return state;
    }

    private <T> T tx(Supplier<T> work) { return transaction.execute(ignored -> work.get()); }
    private static void one(int count) { if (count != 1) throw new IllegalStateException("runner state update unconfirmed"); }
}
