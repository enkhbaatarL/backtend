package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 마스터의 basecase_column_roles 파서 겸 좌표 조회 (패키지 외부 비공개).
 *
 * 예: "B:Year,C:Country,F:% change (+/-)" — 컬럼 문자와 역할 라벨의 순서 목록.
 * 라벨에 콤마/특수문자가 올 수 있으므로 단순 split(",")이 아니라
 * "콤마 뒤에 컬럼문자: 가 오는 위치"에서만 분리한다.
 *
 * payload의 slot 축 키는 이 라벨과 일치한다는 것이 Agent 계약이며,
 * fixed 모드의 value 컬럼은 slot 축으로 매칭되지 않은 잔여 role 하나로 도출한다.
 */
final class ColumnRoleMap {

    /** 콤마 뒤에 "컬럼문자:"가 이어질 때만 role 경계로 본다 (라벨 내 콤마 방어). */
    private static final Pattern ROLE_BOUNDARY = Pattern.compile(",(?=\\s*[A-Za-z]{1,3}:)");

    record Role(int columnIndex, String label) {
    }

    private final String context;
    private final List<Role> roles;

    private ColumnRoleMap(String context, List<Role> roles) {
        this.context = context;
        this.roles = roles;
    }

    static ColumnRoleMap parse(String context, String columnRolesSpec) {
        if (columnRolesSpec == null || columnRolesSpec.isBlank()) {
            throw ValueRows.error(context, "마스터의 column roles가 비어 있습니다");
        }
        List<Role> roles = ROLE_BOUNDARY.splitAsStream(columnRolesSpec)
            .map(token -> toRole(context, token))
            .toList();
        if (roles.isEmpty()) {
            throw ValueRows.error(context, "마스터의 column roles 파싱 결과가 비어 있습니다: %s".formatted(columnRolesSpec));
        }
        return new ColumnRoleMap(context, roles);
    }

    private static Role toRole(String context, String token) {
        int separator = token.indexOf(':');
        if (separator < 1) {
            throw ValueRows.error(context, "마스터의 column role 형식 오류: %s".formatted(token));
        }
        String columnLetter = token.substring(0, separator).trim();
        String label = token.substring(separator + 1).trim();
        if (label.isEmpty()) {
            throw ValueRows.error(context, "마스터의 column role 라벨 누락: %s".formatted(token));
        }
        return new Role(ExcelCells.columnIndex(columnLetter), label);
    }

    List<Role> roles() {
        return roles;
    }

    List<Integer> columnIndexes() {
        return roles.stream().map(Role::columnIndex).toList();
    }

    /** slot 축 키(payload)를 role 라벨로 매칭해 컬럼을 찾는다. 없으면 empty. */
    Optional<Role> byLabel(String label) {
        return roles.stream()
            .filter(role -> role.label().equalsIgnoreCase(label.trim()))
            .findFirst();
    }

    /** slot 축 키로 매칭되지 않은 잔여 role — fixed 모드의 value 컬럼. 정확히 1개가 아니면 에러. */
    Role valueRole(Set<String> slotAxisLabels) {
        List<Role> remaining = roles.stream()
            .filter(role -> slotAxisLabels.stream().noneMatch(label -> role.label().equalsIgnoreCase(label.trim())))
            .toList();
        if (remaining.size() != 1) {
            throw ValueRows.error(context, "value 컬럼을 특정할 수 없습니다 (slot 축으로 안 쓰인 role이 %d개, slot 축=%s)".formatted(remaining.size(), slotAxisLabels));
        }
        return remaining.get(0);
    }

}
