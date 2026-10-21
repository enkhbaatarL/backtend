package com.sk.domain.mi.service.fundamentalmodel.input;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/** 데이터/SQL/저장소 주소 없이 단계와 시간을 기록한다. 요청 uuid는 기존 MDC를 사용한다. */
@Slf4j
final class InputDraftDiagnostics {
    enum Stage {
        CREATE_SOURCE, CREATE_RESUME, CREATE_READ, CREATE_STORE, WORKBOOK_DOWNLOAD, WORKBOOK_PARSE,
        SNAPSHOT_LOCK, SOURCE_LOAD, FIXED_FILES, REVISION_LOAD, INITIAL_ROWS_VALIDATE, ROW_IDENTITIES_LOAD, ROW_POSITIONS_LOAD,
        HISTORY_RESTORE, STRUCTURE_STATE, TABLE_STATE_BEFORE, TABLE_RENDER, TABLE_STATE_AFTER
    }

    private InputDraftDiagnostics() {}

    static <T> T timed(Stage stage, String role, Supplier<T> action) {
        long started = System.nanoTime();
        boolean completed = false;
        try {
            T result = action.get();
            completed = true;
            return result;
        } finally {
            elapsed(stage, role, started, completed);
        }
    }

    static void elapsed(Stage stage, String role, long started, boolean completed) {
        log.info("Input Draft timing stage={} role={} completed={} elapsedMs={}",
            stage, safeRole(role), completed, (System.nanoTime() - started) / 1_000_000);
    }

    private static String safeRole(String role) {
        return role != null && Set.of("DEMAND", "SUPPLY", "TRADE", "OUTPUT").contains(role) ? role : "-";
    }

    static <T> T during(Stage stage, String role, Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException failure) {
            if (!(failure instanceof InputDraftException draft) || draft.getStatus().is5xxServerError()) {
                failure(stage, role, failure);
            }
            throw failure;
        }
    }

    static void failure(Stage stage, String role, Throwable failure) {
        Throwable root = failure;
        SQLException database = null;
        for (int depth = 0; depth < 8; depth++) {
            if (root instanceof SQLException sql) database = sql;
            if (root.getCause() == null || root.getCause() == root) break;
            root = root.getCause();
        }
        String sqlState = database == null ? null : database.getSQLState();
        if (sqlState == null || !sqlState.matches("[A-Z0-9]{5}")) sqlState = "-";
        String source = Arrays.stream(failure.getStackTrace())
            .filter(frame -> frame.getClassName().startsWith("com.sk."))
            .filter(frame -> !frame.getClassName().equals(InputDraftException.class.getName()))
            .limit(3)
            .map(frame -> frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber())
            .collect(Collectors.joining(","));
        // Throwable 자체를 logger에 넘기면 cause/suppressed 메시지에 셀 값이나 SQL이 노출될 수 있다.
        log.error("Input Draft failure stage={} role={} errorType={} causeType={} source={} sqlState={} vendorCode={}",
            stage, safeRole(role), failure.getClass().getName(), root.getClass().getName(), source,
            sqlState, database == null ? 0 : database.getErrorCode());
    }
}
