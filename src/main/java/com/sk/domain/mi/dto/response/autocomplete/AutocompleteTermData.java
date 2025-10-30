package com.sk.domain.mi.dto.response.autocomplete;

import lombok.Getter;

public record AutocompleteTermData(
    @Getter
    String term,

    @Getter
    int frequency) {
}
