package com.sk.common.utils;

import com.sk.domain.shared.mapper.dto.SchemaMetadata;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class DateBindingUtilTest {

    @Test
    void test() {
        SchemaMetadata m = new SchemaMetadata();
        m.setDataType("datetime");
        m.setCharacterMaximumLength(null);

        Pair<String, String> range = DateBindingUtil.range(m, LocalDate.of(2023, 2, 1), LocalDate.of(2023, 12, 14));
        System.out.println(range.getLeft());
        System.out.println(range.getRight());
    }

}
