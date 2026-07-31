package com.sk.domain.mi.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sk.common.handler.GlobalExceptionHandler;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.mi.service.MiDashboardService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("MiDashboardController 테스트")
@ExtendWith(MockitoExtension.class)
class MiDashboardControllerTest {

    private static final String DOWNLOAD_PATH =
            "/api/v1/mi/dashboard/companies/{companyNo}/summary/file-download";
    private static final int COMPANY_NO = 1;
    private static final String BASE_DATE = "20250511";

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private MiDashboardService miDashboardService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MiDashboardController controller = new MiDashboardController(authorizationService, miDashboardService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("predictDate가 없으면 기존처럼 다운로드 요청을 처리한다")
    void downloadWithoutPredictDate() throws Exception {
        // given
        byte[] zipData = "zip-data".getBytes();
        given(miDashboardService.getDashboardSummaryFileDownload(
                eq(COMPANY_NO), eq(BASE_DATE), isNull(), anyString()))
                .willReturn(FileDownloadRes.of(null, "summary.zip", (long) zipData.length, zipData));

        // when & then
        mockMvc.perform(get(DOWNLOAD_PATH, COMPANY_NO)
                        .param("date", BASE_DATE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(zipData));

        then(miDashboardService).should().getDashboardSummaryFileDownload(
                eq(COMPANY_NO), eq(BASE_DATE), isNull(), anyString());
    }

    @Test
    @DisplayName("유효한 predictDate는 다운로드 Service에 전달한다")
    void downloadWithValidPredictDate() throws Exception {
        // given
        byte[] zipData = "zip-data".getBytes();
        given(miDashboardService.getDashboardSummaryFileDownload(
                eq(COMPANY_NO), eq(BASE_DATE), eq("202506"), anyString()))
                .willReturn(FileDownloadRes.of(null, "summary.zip", (long) zipData.length, zipData));

        // when & then
        mockMvc.perform(get(DOWNLOAD_PATH, COMPANY_NO)
                        .param("date", BASE_DATE)
                        .param("predictDate", "202506"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(zipData));

        then(miDashboardService).should().getDashboardSummaryFileDownload(
                eq(COMPANY_NO), eq(BASE_DATE), eq("202506"), anyString());
    }

    @ParameterizedTest(name = "[{index}] predictDate={0}")
    @ValueSource(strings = {"", "x", "202613", "2026-06", "20250601", "202506suffix", " 202506"})
    @DisplayName("잘못된 predictDate는 REQ_INVALID_PARAMETER 400으로 처리한다")
    void rejectInvalidPredictDate(String predictDate) throws Exception {
        // when & then
        mockMvc.perform(get(DOWNLOAD_PATH, COMPANY_NO)
                        .param("date", BASE_DATE)
                        .param("predictDate", predictDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.code").value("REQ-003"));

        then(miDashboardService).shouldHaveNoInteractions();
    }
}
