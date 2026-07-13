package com.sk.domain.mi.service.nextgen.paramproposal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ModelParamSlotAxisResolver {

    private static final Pattern COLUMN_ROLE_BOUNDARY = Pattern.compile(",(?=\\s*[A-Za-z]{1,3}:)");

    /**
     * rendering.columns에서 SLOT으로 해석되는 payload 축을 원래 선언 순서로 반환한다.
     */
    public List<String> resolve(String columnRoles, List<String> declaredSlotAxes) {
        if (declaredSlotAxes == null) {
            throw new InvalidSlotAxisMetadataException();
        }

        Set<String> fixedRoleNames = new LinkedHashSet<>();
        if (columnRoles != null && !columnRoles.isBlank()) {
            for (String token : COLUMN_ROLE_BOUNDARY.split(columnRoles)) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int separator = trimmed.indexOf(':');
                String role = separator < 0 ? trimmed : trimmed.substring(separator + 1).trim();
                if (!role.isEmpty()) {
                    fixedRoleNames.add(role.toLowerCase(Locale.ROOT));
                }
            }
        }

        List<String> resolved = declaredSlotAxes.stream()
            .filter(axis -> axis != null && fixedRoleNames.contains(axis.toLowerCase(Locale.ROOT)))
            .toList();
        if (resolved.isEmpty()) {
            throw new InvalidSlotAxisMetadataException();
        }
        return resolved;
    }

    public static final class InvalidSlotAxisMetadataException extends RuntimeException {

        public InvalidSlotAxisMetadataException() {
            super("Model parameter SLOT-axis metadata is invalid.");
        }
    }
}
