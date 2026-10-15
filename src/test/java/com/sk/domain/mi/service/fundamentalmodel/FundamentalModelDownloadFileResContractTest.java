package com.sk.domain.mi.service.fundamentalmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseFilesRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelDownloadFileRes;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import org.junit.jupiter.api.Test;

class FundamentalModelDownloadFileResContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesDownloadMetadataForAllCaseFileRoles() {
        FundamentalModelCaseFilesRes response = new FundamentalModelCaseFilesRes(
            List.of(new FundamentalModelDownloadFileRes(1001L, "input_1.xlsx", 12345L)),
            List.of(new FundamentalModelDownloadFileRes(2001L, "output.xlsx", 54321L)),
            List.of(new FundamentalModelDownloadFileRes(3001L, "step1_demand_curve.csv", 1234L)),
            List.of(new FundamentalModelDownloadFileRes(4001L, "trade_flow_result.xlsx", 67890L))
        );

        JsonNode json = objectMapper.valueToTree(response);

        assertDownloadMetadata(json.path("input").path(0), 1001L, "input_1.xlsx", 12345L);
        assertDownloadMetadata(json.path("output").path(0), 2001L, "output.xlsx", 54321L);
        assertDownloadMetadata(
            json.path("intermediate").path(0),
            3001L,
            "step1_demand_curve.csv",
            1234L
        );
        assertDownloadMetadata(
            json.path("tradeOutput").path(0),
            4001L,
            "trade_flow_result.xlsx",
            67890L
        );
    }

    @Test
    void exposesDownloadFileSchema() {
        var schemas = ModelConverters.getInstance()
            .read(FundamentalModelDownloadFileRes.class);
        Schema<?> schema = schemas.get("FundamentalModelDownloadFileRes");

        assertThat(schemas).containsKey("FundamentalModelDownloadFileRes");
        assertThat(schema.getProperties())
            .containsOnlyKeys("fileNo", "fileNm", "fileSize");
    }

    @Test
    void exposesDownloadMetadataArraysInOpenApiSchema() {
        var schemas = ModelConverters.getInstance()
            .read(FundamentalModelCaseFilesRes.class);
        Schema<?> schema = schemas.get("FundamentalModelCaseFilesRes");
        Schema<?> inputSchema = (Schema<?>) schema.getProperties().get("input");
        Schema<?> outputSchema = (Schema<?>) schema.getProperties().get("output");
        Schema<?> intermediateSchema = (Schema<?>) schema.getProperties().get("intermediate");
        Schema<?> tradeOutputSchema = (Schema<?>) schema.getProperties().get("tradeOutput");

        assertThat(schemas).containsKey("FundamentalModelCaseFilesRes");
        assertThat(schema.getProperties())
            .containsKeys("input", "output", "intermediate", "tradeOutput");
        assertThat(schema.getRequired()).contains("tradeOutput");
        assertThat(inputSchema.getItems().get$ref())
            .isEqualTo("#/components/schemas/FundamentalModelDownloadFileRes");
        assertThat(outputSchema.getItems().get$ref())
            .isEqualTo("#/components/schemas/FundamentalModelDownloadFileRes");
        assertThat(intermediateSchema.getItems().get$ref())
            .isEqualTo("#/components/schemas/FundamentalModelDownloadFileRes");
        assertThat(tradeOutputSchema.getItems().get$ref())
            .isEqualTo("#/components/schemas/FundamentalModelDownloadFileRes");
        assertThat(tradeOutputSchema.getDescription())
            .contains("Trade Flow", "같은 run_seq", "빈 배열");
    }

    private static void assertDownloadMetadata(
        JsonNode file,
        long expectedFileNo,
        String expectedFileName,
        long expectedFileSize
    ) {
        assertThat(file.path("fileNo").asLong()).isEqualTo(expectedFileNo);
        assertThat(file.path("fileNm").asText()).isEqualTo(expectedFileName);
        assertThat(file.path("fileSize").asLong()).isEqualTo(expectedFileSize);
        assertThat(file.size()).isEqualTo(3);
    }
}
