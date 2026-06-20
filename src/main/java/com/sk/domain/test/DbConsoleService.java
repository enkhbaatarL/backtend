package com.sk.domain.test;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * DB Console 실행 서비스.
 *
 * <p>로컬에서 직접 접속 불가한 서버 DB를 백엔드 경유로 조회/수정하기 위한 관리자 전용 도구.
 * master 데이터소스에 직접 SQL을 실행한다.
 *
 * <p>안전장치:
 * <ul>
 *   <li>허용 구문: SELECT / SHOW / DESCRIBE / EXPLAIN (read), INSERT / UPDATE / DELETE (write)</li>
 *   <li>차단: DDL / 멀티 스테이트먼트 / WHERE 없는 UPDATE·DELETE</li>
 *   <li>조회 최대 {@value #MAX_ROWS}행, 쿼리 타임아웃 {@value #QUERY_TIMEOUT_SEC}초</li>
 *   <li>실행 SQL·실행자·결과 건수 audit 로깅</li>
 * </ul>
 */
@Slf4j
@Service
public class DbConsoleService {

    private static final int MAX_ROWS = 2000;
    private static final int QUERY_TIMEOUT_SEC = 30;

    private static final Set<String> READ_STATEMENTS = Set.of("SELECT", "SHOW", "DESCRIBE", "DESC", "EXPLAIN");
    private static final Set<String> WRITE_STATEMENTS = Set.of("INSERT", "UPDATE", "DELETE");

    private final JdbcTemplate jdbcTemplate;
    private final AuthorizationService authorizationService;

    public DbConsoleService(
        @Qualifier("masterDataSource") DataSource masterDataSource,
        AuthorizationService authorizationService
    ) {
        this.jdbcTemplate = new JdbcTemplate(masterDataSource);
        this.jdbcTemplate.setMaxRows(MAX_ROWS);
        this.jdbcTemplate.setQueryTimeout(QUERY_TIMEOUT_SEC);
        this.authorizationService = authorizationService;
    }

    public DbConsoleResult execute(String rawSql) {
        String sql = normalize(rawSql);
        String head = firstKeyword(sql);

        boolean read = READ_STATEMENTS.contains(head);
        boolean write = WRITE_STATEMENTS.contains(head);
        if (!read && !write) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID,
                "허용되지 않은 구문입니다: '%s' (SELECT/SHOW/DESCRIBE/EXPLAIN/INSERT/UPDATE/DELETE 만 가능)".formatted(head));
        }
        if (("UPDATE".equals(head) || "DELETE".equals(head)) && !sql.toUpperCase().contains("WHERE")) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "WHERE 절 없는 %s는 차단됩니다.".formatted(head));
        }

        long start = System.nanoTime();
        try {
            if (read) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                audit(head, rows.size(), sql);
                return new DbConsoleResult("READ", columns, rows, null, rows.size(), rows.size() >= MAX_ROWS, elapsedMs);
            }

            int affected = jdbcTemplate.update(sql);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            audit(head, affected, sql);
            return new DbConsoleResult("WRITE", null, null, affected, affected, false, elapsedMs);

        } catch (DataAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "SQL 실행 오류: " + cause.getMessage());
        }
    }

    private String normalize(String rawSql) {
        String sql = rawSql == null ? "" : rawSql.strip();
        if (sql.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "SQL이 비어 있습니다.");
        }
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).strip();
        }
        if (sql.contains(";")) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "멀티 스테이트먼트(여러 SQL)는 실행할 수 없습니다.");
        }
        return sql;
    }

    /** 선행 키워드 추출. 주석·괄호 등 비알파벳으로 시작하면 빈 문자열 → 차단된다. */
    private String firstKeyword(String sql) {
        int i = 0;
        while (i < sql.length() && Character.isLetter(sql.charAt(i))) {
            i++;
        }
        return sql.substring(0, i).toUpperCase();
    }

    private void audit(String operation, int count, String sql) {
        String who;
        try {
            who = authorizationService.getSubject();
        } catch (Exception e) {
            who = "unknown";
        }
        log.warn("[DB-CONSOLE] user={}, op={}, count={}, sql={}", who, operation, count, sql);
    }

}
