package com.sk.domain.mi.dto.response.newsletter;

import java.util.List;

public record RevisionListRes(
    List<RevisionItemRes> items,
    Boolean hasNext,
    Long nextCursor
) {
}
