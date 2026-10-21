package com.sk.domain.mi.service.fundamentalmodel.input;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.mi.controller.fundamentalmodel.ModelInputDraftController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes=ModelInputDraftOpenApiTest.TestApplication.class,properties={"springdoc.api-docs.enabled=true","springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc(addFilters=false)
class ModelInputDraftOpenApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean ModelInputDraftService service;
    @Test void generatedContractUsesJsonTypedBodiesAndFlattenedDraftResponse()throws Exception {
        JsonNode api=generated();
        String root="/api/v1/mi/fundamental-model";
        JsonNode create=api.path("paths").path(root+"/cases/{caseId}/input-drafts").path("post");
        assertThat(create.path("requestBody").path("required").asBoolean()).isTrue();
        String bodyRef=create.path("requestBody").path("content").path("application/json").path("schema").path("$ref").asText();
        JsonNode body=resolve(api,bodyRef);assertThat(body.path("properties").path("sourceVersion").path("pattern").asText()).isEqualTo("^sv1_[0-9a-f]{64}$");
        String responseRef=create.path("responses").path("200").path("content").path("application/json").path("schema").path("$ref").asText();
        JsonNode envelope=resolve(api,responseRef);JsonNode data=resolve(api,envelope.path("properties").path("data").path("$ref").asText());
        assertThat(data.path("properties").has("draftId")).isTrue();assertThat(data.path("properties").has("summary")).isFalse();
        assertThat(data.path("properties").has("created")).isTrue();
        assertThat(create.path("responses").has("413")).isTrue();
        JsonNode table=api.path("paths").path(root+"/input-drafts/{draftId}/table-data").path("get");
        assertThat(table.path("responses").has("422")).isTrue();assertThat(table.path("description").asText()).contains("2,000", "2MiB");
        assertThat(api.path("paths").path(root+"/input-upload-drafts").path("post").path("description").asText())
            .contains("처리 기한600초");
        for(JsonNode parameter:table.path("parameters"))if("revision".equals(parameter.path("name").asText()))assertThat(parameter.path("required").asBoolean()).isTrue();
    }
    @Test void generatedNestedContractMarksRequiredAndNullablePageRowAndErrorFields()throws Exception {
        JsonNode api=generated();
        JsonNode schemas=api.path("components").path("schemas");
        JsonNode page=schemas.path("Page");
        assertThat(strings(page.path("required"))).containsExactlyInAnyOrder("totalBodyRows","returnedBodyRows","startRow","endRow","hasNext","nextStartRow");
        for(String name:java.util.List.of("startRow","endRow","nextStartRow"))assertThat(nullable(page.path("properties").path(name))).as(name+" "+page.path("properties").path(name)).isTrue();
        JsonNode binding=schemas.path("RowBinding");
        assertThat(strings(binding.path("required"))).containsExactlyInAnyOrder("rowNumber","rowId","origin");
        assertThat(nullable(binding.path("properties").path("rowId"))).isTrue();
        assertThat(strings(binding.path("properties").path("origin").path("enum"))).containsExactly("SOURCE","ADDED","EMPTY_SLOT");
        assertThat(schemas.path("Table").path("properties").path("revision").path("maximum").asLong()).isEqualTo(9007199254740991L);
        assertThat(strings(schemas.path("Editing").path("required"))).containsExactlyInAnyOrder("capabilities","fields","rowBindings","editValues");
        JsonNode problem=schemas.path("ProblemEnvelope").path("properties").path("data");
        assertThat(nullable(problem)).isTrue();
        // 3.1의 $ref와 type:null을 나란히 쓰면 참조 대상 object 제약 때문에 null이 거부된다.
        assertThat(problem.has("$ref")).isFalse();
        assertThat(nullable(problem.path("properties").path("currentRevision"))).isTrue();
        assertThat(strings(problem.path("required"))).containsExactlyInAnyOrder("reason","currentRevision","issues");
        for(String field:java.util.List.of("activeOperationId","activeOperationEndpoint","finalizedCaseId"))
            assertThat(nullable(schemas.path("Created").path("properties").path(field))).as(field).isTrue();
        assertThat(strings(schemas.path("ProblemEnvelope").path("required"))).containsExactlyInAnyOrder("status","code","message","data");
        for(String name:java.util.List.of("ProblemEnvelope","NullErrorEnvelope")) {
            JsonNode status=schemas.path(name).path("properties").path("status");
            assertThat(status.path("type").asText()).isEqualTo("boolean");
            assertThat(status.has("enum")).as("boolean false를 문자열 enum으로 제한하지 않음").isFalse();
        }
        String root="/api/v1/mi/fundamental-model";
        for(String path:java.util.List.of(root+"/cases/{caseId}/input-drafts",root+"/input-drafts/{draftId}",root+"/input-drafts/{draftId}/table-data")) {
            String method=path.endsWith("input-drafts")?"post":"get";
            JsonNode responses=api.path("paths").path(path).path(method).path("responses");
            for(String code:java.util.List.of("400","404","409","500"))assertThat(responses.path(code).path("content").path("application/json").path("schema").path("$ref").asText())
                .as(path+" "+code).endsWith("/ProblemEnvelope");
        }
        JsonNode source409=api.path("paths").path(root+"/cases/{caseId}/inputs/structure").path("get").path("responses").path("409");
        assertThat(source409.path("content").path("application/json").path("schema").path("$ref").asText()).endsWith("/NullErrorEnvelope");
        assertThat(nullable(schemas.path("NullErrorEnvelope").path("properties").path("data"))).isTrue();
    }
    private java.util.List<String> strings(JsonNode array){java.util.List<String> result=new java.util.ArrayList<>();array.forEach(v->result.add(v.asText()));return result;}
    private boolean nullable(JsonNode node){return node.path("nullable").asBoolean()||"null".equals(node.path("type").asText())||strings(node.path("type")).contains("null")
        ||node.path("anyOf").findValuesAsText("type").contains("null");}
    private JsonNode generated()throws Exception {
        byte[] bytes=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        java.nio.file.Path artifact=java.nio.file.Path.of("build/generated-openapi/model-input-drafts.json");
        java.nio.file.Files.createDirectories(artifact.getParent());java.nio.file.Files.write(artifact,bytes);
        return mapper.readTree(bytes);
    }
    private JsonNode resolve(JsonNode api,String ref){assertThat(ref).startsWith("#/components/schemas/");return api.path("components").path("schemas").path(ref.substring(ref.lastIndexOf('/')+1));}
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,DataSourceTransactionManagerAutoConfiguration.class,HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.class,Saml2RelyingPartyAutoConfiguration.class,
        SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class})
    @Import({ModelInputDraftController.class,InputDraftJson.class})
    static class TestApplication {}
}
