package com.sk.domain.shared.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.Builder;

@Builder
public record ContentProviderAccess (
    @JsonProperty("content_provider")
    String contentProvider,
    @JsonProperty("authorized_product_nums")
    Set<Integer> authGdsList
){
    public static ContentProviderAccess of(String contentProvider, Set<Integer> authGdsList) {
        return ContentProviderAccess.builder()
            .contentProvider(contentProvider)
            .authGdsList(authGdsList)
            .build();
    }
}
