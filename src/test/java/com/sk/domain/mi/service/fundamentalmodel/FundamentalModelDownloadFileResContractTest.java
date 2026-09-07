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
    void serializesCaseFilesWithFileNameOnly() {
        FundamentalModelCaseFilesRes response = new FundamentalModelCaseFilesRes(
            List.of(new FundamentalModelDownloadFileRes("input_1.xlsx")),
            List.of(new FundamentalModelDownloadFileRes("output.xlsx")),
            List.of(new FundamentalModelDownloadFileRes("step1_demand_curve.csv"))
        );

        JsonNode json = objectMapper.valueToTree(response);

        assertFileNameOnly(json.path("input").path(0), "input_1.xlsx");
        assertFileNameOnly(json.path("output").path(0), "output.xlsx");
        assertFileNameOnly(json.path("intermediate").path(0), "step1_demand_curve.csv");
    }

    @Test
    void exposesOnlyFileNameInOpenApiSchema() {
        var schemas = ModelConverters.getInstance()
            .read(FundamentalModelDownloadFileRes.class);
        Schema<?> schema = schemas.get("FundamentalModelDownloadFileRes");

        assertThat(schemas).containsKey("FundamentalModelDownloadFileRes");
        assertThat(schema.getProperties())
            .containsOnlyKeys("fileNm");
    }

    @Test
    void exposesIntermediateInOpenApiSchema() {
        var schemas = ModelConverters.getInstance()
            .read(FundamentalModelCaseFilesRes.class);
        Schema<?> schema = schemas.get("FundamentalModelCaseFilesRes");
        Schema<?> intermediateSchema = (Schema<?>) schema.getProperties().get("intermediate");

        assertThat(schemas).containsKey("FundamentalModelCaseFilesRes");
        assertThat(schema.getProperties())
            .containsKeys("input", "output", "intermediate");
        assertThat(intermediateSchema.getItems().get$ref())
            .isEqualTo("#/components/schemas/FundamentalModelDownloadFileRes");
    }

    private static void assertFileNameOnly(JsonNode file, String expectedFileName) {
        assertThat(file.path("fileNm").asText()).isEqualTo(expectedFileName);
        assertThat(file.has("fileNo")).isFalse();
        assertThat(file.has("fileSize")).isFalse();
        assertThat(file.size()).isEqualTo(1);
    }
}
