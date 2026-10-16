package com.sk.domain.shared.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [MCP Feedback] 피드백 대상 종류 (tcm_usrfedbl.fedb_sectn_nm). 값은 화면 상세 페이지가 보내는 섹션명과 같다.
 * fedb_trgt_id 가 어떤 ID 인지는 이 값이 정한다.
 */
@Getter
@RequiredArgsConstructor
public enum FeedbackSectionNm {

    EVENT("Event", List.of("event", "events", "이벤트")),
    SCENARIO_GROUP("Scenario Group", List.of("scenariogroup", "scenariogroups", "group", "시나리오그룹", "그룹")),
    SCENARIO("Scenario", List.of("scenario", "scenarios", "시나리오"));

    private final String value;
    private final List<String> aliases;

    /**
     * 대소문자·공백·하이픈·밑줄을 무시하고 섹션명을 찾는다. 찾지 못하면 empty.
     */
    public static Optional<FeedbackSectionNm> fromLenient(String input) {
        String key = normalize(input);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
            .filter(section -> normalize(section.value).equals(key)
                || normalize(section.name()).equals(key)
                || section.aliases.contains(key))
            .findFirst();
    }

    private static String normalize(String input) {
        return input == null ? "" : input.replaceAll("[\\s_\\-/]", "").toLowerCase(Locale.ROOT);
    }

}
