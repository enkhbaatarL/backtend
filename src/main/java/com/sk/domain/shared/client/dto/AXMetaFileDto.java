package com.sk.domain.shared.client.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

// @formatter:off
@Builder
public record AXMetaFileDto(
    @JsonProperty("GdsNo")
    Integer gdsNo,

    @JsonProperty("DocumentId")
    String documentId,

    @JsonProperty("DocumentReleaseDate")
    String documentReleaseDate,

    @JsonProperty("Headline")
    String headline,

    @JsonProperty("Synopsis")
    String synopsis,

    @JsonProperty("Contributor")
    String contributor,

    @JsonProperty("Authors")
    List<String> authors,

    @JsonProperty("DocumentFileName")
    List<String> documentFileName,

    @JsonProperty("Language")
    String language,

    @JsonProperty("Countries")
    List<String> countries)
{

}
// @formatter:on