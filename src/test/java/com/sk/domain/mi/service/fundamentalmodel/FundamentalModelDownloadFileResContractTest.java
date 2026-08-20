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
    void serializesInputAndOutputWithFileNameOnly() {
        FundamentalModelCaseFilesRes response = new FundamentalModelCaseFilesRes(
            List.of(new FundamentalModelDownloadFileRes("input_1.xlsx")),
            List.of(new FundamentalModelDownloadFileRes("output.xlsx"))
        );

        JsonNode json = objectMapper.valueToTree(response);

        assertFileNameOnly(json.path("input").path(0), "input_1.xlsx");
        assertFileNameOnly(json.path("output").path(0), "output.xlsx");
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

    private static void assertFileNameOnly(JsonNode file, String expectedFileName) {
        assertThat(file.path("fileNm").asText()).isEqualTo(expectedFileName);
        assertThat(file.has("fileNo")).isFalse();
        assertThat(file.has("fileSize")).isFalse();
        assertThat(file.size()).isEqualTo(1);
    }
}
