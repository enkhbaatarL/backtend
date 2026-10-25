package com.sk.common.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class P6SpyFormatter implements MessageFormattingStrategy {

    private static final int SQL_SHOW_LIMIT_LENGTH = 5000;

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(this.getClass().getName());
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (prepared != null && prepared.toLowerCase(Locale.ROOT).matches("(?s).*\\btmd_mdl_inptsnap(?:m|d|jsond)\\b.*")) {
            return String.format("[%s] | %d ms | Table Snapshot SQL [parameters redacted]", category, elapsed);
        }
        sql = formatSql(category, sql);

        if (sql != null && sql.length() > SQL_SHOW_LIMIT_LENGTH) {
            return String.format("[%s] | %d ms | %s...", category, elapsed, sql.substring(0, SQL_SHOW_LIMIT_LENGTH));
        }

        return String.format("[%s] | %d ms | %s", category, elapsed, sql);
    }

    private String formatSql(String category, String sql) {
        if (sql != null && !sql.trim().isEmpty() && Category.STATEMENT.getName().equals(category)) {
            String trimmedSQL = sql.trim().toLowerCase(Locale.ROOT);
            if (trimmedSQL.startsWith("create") || trimmedSQL.startsWith("alter") || trimmedSQL.startsWith("comment")) {
                sql = FormatStyle.DDL.getFormatter().format(sql);
            } else {
                sql = FormatStyle.BASIC.getFormatter().format(sql);
            }
            return sql;
        }
        return sql;
    }

}
