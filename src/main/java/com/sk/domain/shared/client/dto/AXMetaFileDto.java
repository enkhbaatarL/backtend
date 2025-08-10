package com.sk.domain.shared.client.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

// @formatter:off
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

    @Builder
    public AXMetaFileDto(Integer gdsNo, String documentId, String documentReleaseDate, String headline, String synopsis,
            String contributor, List<String> authors, List<String> documentFileName, String language, List<String> countries) {
        this.gdsNo = gdsNo;
        this.documentId = documentId;
        this.documentReleaseDate = documentReleaseDate;
        this.headline = headline;
        this.synopsis = synopsis;
        this.contributor = contributor;
        this.authors = authors;
        this.documentFileName = documentFileName;
        this.language = language;
        this.countries = countries;

    } 
}
// @formatter:on