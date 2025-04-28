package com.sk.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

public record PaginationTableRes<T, R>(

    @Schema(description = "전체 페이지 수", example = "8")
    int totalPages,

    @Schema(description = "전체 데이터 개수", example = "1234")
    int totalElements,

    @Schema(description = "현재 페이지", example = "3")
    int currentPage,

    @Schema(description = "현재 페이지에서 조회된 데이터 개수", example = "1234")
    int numberOfElements,

    @Schema(description = "한 페이지당 최대 데이터 개수", example = "10")
    int size,

    @Schema(description = "첫번째 페이지 여부", example = "false")
    boolean isFirst,

    @Schema(description = "마지막 페이지 여부", example = "false")
    boolean isLast,

    @Schema(description = "이전 페이지 존재 여부", example = "false")
    boolean hasPrevious,

    @Schema(description = "다음 페이지 존재 여부", example = "false")
    boolean hasNext,

    @Schema(description = "ROW 상세")
    List<T> detail,

    @Schema(description = "HEADER 정보")
    List<R> headers

) {

    public static <T, R> PaginationTableRes<T, R> of(int totalPages, int totalElements, int currentPage, int numberOfElements, int size, boolean isFirst, boolean isLast, boolean hasPrevious, boolean hasNext, List<T> detail, List<R> headers) {
        return new PaginationTableRes<>(totalPages, totalElements, currentPage, numberOfElements, size, isFirst, isLast, hasPrevious, hasNext, detail, headers);
    }

    public static <T, R> PaginationTableRes<T, R> toRes(int reqPage, int reqSize, int totalElements, List<T> detail, List<R> headers) {

        int totalPages = (int) Math.ceil((double) totalElements / reqSize);

        return PaginationTableRes.of(
            totalPages,
            totalElements,
            reqPage,
            detail.size(),
            reqSize,
            reqSize == 1,
            reqSize >= totalPages,
            reqSize > 1,
            reqSize < totalPages,
            detail,
            headers
        );
    }

}
