package com.sk.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.domain.admin.service.AdminBulletinBoardService;
import com.sk.domain.admin.validator.NoticeImageUploadValidator;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = AdminBulletinBoardOpenApiTest.TestApplication.class,
        properties = {
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=false"
        })
@AutoConfigureMockMvc(addFilters = false)
class AdminBulletinBoardOpenApiTest {

    private static final String UPLOAD_PATH =
            "/api/v1/admin/bulletinboard-management/notice/upload-file";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private AdminBulletinBoardService adminBulletinBoardService;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private NoticeImageUploadValidator noticeImageUploadValidator;

    @Test
    void exposesTypedSuccessAndStructuredValidationErrors() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode operation = objectMapper.readTree(document)
                .path("paths")
                .path(UPLOAD_PATH)
                .path("post");
        assertThat(operation.path("description").asText()).contains("오류 코드를 반환합니다");

        JsonNode successContent = operation.path("responses").path("200").path("content");
        assertThat(fieldNames(successContent)).containsExactly("application/json");
        assertThat(successContent.path("application/json").path("schema").path("$ref").asText())
                .endsWith("/BaseResponseFileUploadRes");

        JsonNode badRequestContent = operation.path("responses").path("400").path("content");
        assertThat(fieldNames(badRequestContent)).containsExactly("application/json");
        assertThat(badRequestContent.path("application/json").path("schema").path("$ref").asText())
                .endsWith("/BaseResponse");

        JsonNode examples = badRequestContent.path("application/json").path("examples");
        assertThat(fieldNames(examples)).containsExactlyInAnyOrder(
                "REQ_INVALID_PARAMETER",
                "REQ_INVALID",
                "FILE_INVALID",
                "FILE_FORMAT_ERROR",
                "FILE_SIZE_ERROR");

        Map<String, ResultCode.Error> expectedErrors = new LinkedHashMap<>();
        expectedErrors.put("REQ_INVALID_PARAMETER", ResultCode.Error.REQ_INVALID_PARAMETER);
        expectedErrors.put("REQ_INVALID", ResultCode.Error.REQ_INVALID);
        expectedErrors.put("FILE_INVALID", ResultCode.Error.FILE_INVALID);
        expectedErrors.put("FILE_FORMAT_ERROR", ResultCode.Error.FILE_FORMAT_ERROR);
        expectedErrors.put("FILE_SIZE_ERROR", ResultCode.Error.FILE_SIZE_ERROR);

        expectedErrors.forEach((name, expected) -> {
            JsonNode value = examples.path(name).path("value");
            assertThat(value.path("status").asBoolean()).isFalse();
            assertThat(value.path("code").asText()).isEqualTo(expected.getCode());
            assertThat(value.path("message").asText()).isEqualTo(expected.getMessage());
            assertThat(value.path("data").isNull()).isTrue();
        });
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.class,
            Saml2RelyingPartyAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class
    })
    @Import(AdminBulletinBoardController.class)
    static class TestApplication {
    }
}
