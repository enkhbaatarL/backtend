package com.sk.domain.test;

/**
 * DB Console 실행 요청.
 *
 * @param sql 실행할 단일 SQL (SELECT/SHOW/DESCRIBE/EXPLAIN/INSERT/UPDATE)
 */
public record DbConsoleExecuteReq(String sql) {
}
