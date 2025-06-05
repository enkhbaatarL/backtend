package com.sk.common.utils;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.shared.enums.DateGranularity;
import com.sk.domain.shared.mapper.dto.SchemaMetadata;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class DateBindingUtil {

    private static final DateTimeFormatter F_Y = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter F_YM = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter F_YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter F_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Granularity 판정
     */
    public static DateGranularity resolve(SchemaMetadata m) {
        String dt = m.getDataType().toLowerCase();
        Integer len = m.getCharacterMaximumLength();

        return switch (dt) {
            case "int" -> DateGranularity.YEAR_INT;
            case "varchar" -> switch (len) {
                case 4 -> DateGranularity.YEAR_VARCHAR;
                case 6 -> DateGranularity.MONTH_VARCHAR;
                case 8 -> DateGranularity.DAY_VARCHAR;
                default -> throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "unsupported data column: %s, length: %s".formatted(dt, len));
            };
            case "date", "datetime", "timestamp" -> DateGranularity.DATETIME;
            default -> throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "unsupported data column: %s".formatted(dt));
        };
    }

    /**
     * 범위 값 변환
     * yyyy
     * yyyyMM
     * yyyyMMdd
     */
    public static Pair<String, String> range(SchemaMetadata meta, LocalDate start, LocalDate end) {
        return switch (resolve(meta)) {
            case YEAR_INT, YEAR_VARCHAR -> Pair.of(F_Y.format(start), F_Y.format(end));
            case MONTH_VARCHAR -> Pair.of(F_YM.format(YearMonth.from(start)), F_YM.format(YearMonth.from(end)));
            case DAY_VARCHAR -> Pair.of(F_YMD.format(start), F_YMD.format(end));
            case DATETIME -> Pair.of(F_TS.format(start.atStartOfDay()), F_TS.format(end.atTime(LocalTime.MAX)));
        };
    }

    /**
     * 단일 값 변환
     */
    public static String single(SchemaMetadata meta, LocalDate date) {
        return switch (resolve(meta)) {
            case YEAR_INT, YEAR_VARCHAR -> F_Y.format(date);
            case MONTH_VARCHAR -> F_YM.format(YearMonth.from(date));
            case DAY_VARCHAR -> F_YMD.format(date);
            case DATETIME -> F_TS.format(date.atStartOfDay());
        };
    }

}
