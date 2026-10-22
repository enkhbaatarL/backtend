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
        HISTORY_RESTORE, STRUCTURE_STATE, TABLE_STATE_BEFORE, TABLE_RENDER, TABLE_STATE_AFTER,
        CHANGE_SUBMIT, CHANGE_PREPARE, CHANGE_EXECUTE, CHANGE_COMPLETE, CHANGE_STATUS, CHANGE_STATUS_EXPIRE,
        CHANGE_REPLAY, CHANGE_TRANSFORM, CHANGE_APPLY, WORKBOOK_SOURCE_VALIDATE, WORKBOOK_ZIP_WRITE,
        WORKBOOK_SAVED_VALIDATE, WORKBOOK_PRESERVATION_VERIFY, WORKBOOK_READ_BACK
    }

    private static final ThreadLocal<String> OPERATION = new ThreadLocal<>();

    static <T> T withOperation(String operation, Supplier<T> action) {
        String previous = OPERATION.get();
        OPERATION.set(operation != null && operation.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") ? operation : "-");
        try { return action.get(); }
        finally { if (previous == null) OPERATION.remove(); else OPERATION.set(previous); }
    }

    @FunctionalInterface interface CheckedSupplier<T> { T get() throws Exception; }

    static <T> T timedChecked(Stage stage, String role, CheckedSupplier<T> action) throws Exception {
        long started = System.nanoTime(); boolean completed = false;
        try { T result = action.get(); completed = true; return result; }
        finally { elapsed(stage, role, started, completed); }
    }

    static void rows(int identities, int positions) {
        log.info("Input Draft rows operationId={} identities={} positions={}", OPERATION.get() == null ? "-" : OPERATION.get(), identities, positions);
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
        long millis = (System.nanoTime() - started) / 1_000_000;
        if (OPERATION.get() == null) log.info("Input Draft timing stage={} role={} completed={} elapsedMs={}", stage, safeRole(role), completed, millis);
        else log.info("Input Draft timing stage={} role={} completed={} elapsedMs={} operationId={}", stage, safeRole(role), completed, millis, OPERATION.get());
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
