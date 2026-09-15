package com.sk.domain.mi.service.fundamentalmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseFilesRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelDownloadFileRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelFileNameRes;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import org.junit.jupiter.api.Test;

class FundamentalModelDownloadFileResContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesRoleSpecificCaseFileMetadata() {
        FundamentalModelCaseFilesRes response = new FundamentalModelCaseFilesRes(
            List.of(new FundamentalModelFileNameRes("input_1.xlsx")),
            List.of(new FundamentalModelDownloadFileRes(2001L, "output.xlsx", 54321L)),
            List.of(new FundamentalModelDownloadFileRes(3001L, "step1_demand_curve.csv", 1234L))
        );

        JsonNode json = objectMapper.valueToTree(response);

        assertFileNameOnly(json.path("input").path(0), "input_1.xlsx");
        assertDownloadMetadata(json.path("output").path(0), 2001L, "output.xlsx", 54321L);
        assertDownloadMetadata(
            json.path("intermediate").path(0),
            3001L,
            "step1_demand_curve.csv",
            1234L
        );
    }

    @Test
    void exposesRoleSpecificFileSchemas() {
        var fileNameSchemas = ModelConverters.getInstance()
            .read(FundamentalModelFileNameRes.class);
        Schema<?> fileNameSchema = fileNameSchemas.get("FundamentalModelFileNameRes");
        var schemas = ModelConverters.getInstance()
            .read(FundamentalModelDownloadFileRes.class);
        Schema<?> schema = schemas.get("FundamentalModelDownloadFileRes");

        assertThat(fileNameSchemas).containsKey("FundamentalModelFileNameRes");
        assertThat(fileNameSchema.getProperties())
            .containsOnlyKeys("fileNm");
        assertThat(schemas).containsKey("FundamentalModelDownloadFileRes");
        assertThat(schema.getProperties())
            .containsOnlyKeys("fileNo", "fileNm", "fileSize");
    }

    @Test
    void exposesIntermediateInOpenApiSchema() {
        var schemas = ModelConverters.getInstance()
            .read(FundamentalModelCaseFilesRes.class);
        Schema<?> schema = schemas.get("FundamentalModelCaseFilesRes");
        Schema<?> inputSchema = (Schema<?>) schema.getProperties().get("input");
        Schema<?> outputSchema = (Schema<?>) schema.getProperties().get("output");
        Schema<?> intermediateSchema = (Schema<?>) schema.getProperties().get("intermediate");

        assertThat(schemas).containsKey("FundamentalModelCaseFilesRes");
        assertThat(schema.getProperties())
            .containsKeys("input", "output", "intermediate");
        assertThat(inputSchema.getItems().get$ref())
            .isEqualTo("#/components/schemas/FundamentalModelFileNameRes");
        assertThat(outputSchema.getItems().get$ref())
            .isEqualTo("#/components/schemas/FundamentalModelDownloadFileRes");
        assertThat(intermediateSchema.getItems().get$ref())
            .isEqualTo("#/components/schemas/FundamentalModelDownloadFileRes");
    }

    private static void assertFileNameOnly(JsonNode file, String expectedFileName) {
        assertThat(file.path("fileNm").asText()).isEqualTo(expectedFileName);
        assertThat(file.has("fileNo")).isFalse();
        assertThat(file.has("fileSize")).isFalse();
        assertThat(file.size()).isEqualTo(1);
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
