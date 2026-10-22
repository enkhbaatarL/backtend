package com.sk.domain.mi.service.fundamentalmodel.input;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/** 잠금은 직렬화 수단이며 이전 Java/전송 종료 증거가 아니다. 테스트에서는 이 경계만 대체한다. */
interface InputDraftRunnerLease extends AutoCloseable {
    boolean held() throws Exception;
    @Override void close() throws Exception;

    interface Factory { InputDraftRunnerLease acquire() throws Exception; }

    static Factory mysql(DataSource master) { return () -> Mysql.acquire(master); }

    final class Mysql implements InputDraftRunnerLease {
        private final Connection connection;
        private boolean closed;

        private Mysql(Connection connection) { this.connection = connection; }

        static InputDraftRunnerLease acquire(DataSource source) throws SQLException {
            Connection connection = source.getConnection();
            Mysql lease = new Mysql(connection);
            try {
                connection.setAutoCommit(true);
                if (lease.query("SELECT GET_LOCK(CONCAT('mi-input-draft:',DATABASE()),0)")) return lease;
                connection.close();
                return null;
            } catch (SQLException | RuntimeException ex) {
                // GET_LOCK 응답 유실 때도 물리 세션을 끊어 pool에 잠금이 남지 않게 한다.
                lease.abort();
                throw ex;
            }
        }

        @Override public synchronized boolean held() throws SQLException {
            return !closed && query("SELECT IS_USED_LOCK(CONCAT('mi-input-draft:',DATABASE()))=CONNECTION_ID()");
        }

        private boolean query(String statement) throws SQLException {
            try (var query = connection.prepareStatement(statement)) {
                query.setQueryTimeout(2);
                try (var rows = query.executeQuery()) { return rows.next() && rows.getInt(1) == 1 && !rows.wasNull(); }
            }
        }

        @Override public synchronized void close() throws SQLException {
            if (closed) return;
            closed = true;
            try {
                query("SELECT RELEASE_LOCK(CONCAT('mi-input-draft:',DATABASE()))");
                connection.close();
            } catch (SQLException ex) { abort(); throw ex; }
        }

        private void abort() throws SQLException {
            try { connection.abort(Runnable::run); }
            finally { connection.close(); }
        }
    }
}
