package com.sk.domain.mi.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sk.common.handler.GlobalExceptionHandler;
import com.sk.domain.mi.service.MiCompanyService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("MiCompanyController 테스트")
@ExtendWith(MockitoExtension.class)
class MiCompanyControllerTest {

    private static final String DETAIL_PATH = "/api/v1/mi/company-management/companies/{companyNo}/detail";
    private static final int OWN_COMPANY_NO = 1;
    private static final int OTHER_COMPANY_NO = 2;

    @Mock
    private MiCompanyService miCompanyService;

    @Mock
    private AuthorizationService authorizationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MiCompanyController controller = new MiCompanyController(miCompanyService, authorizationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("일반 사용자는 자기 회사 상세를 조회할 수 있다")
    void userCanReadOwnCompany() throws Exception {
        given(authorizationService.getCurrentRole()).willReturn(Role.USER);
        given(authorizationService.getCompanyNo()).willReturn(OWN_COMPANY_NO);

        mockMvc.perform(get(DETAIL_PATH, OWN_COMPANY_NO))
                .andExpect(status().isOk());

        then(miCompanyService).should().getCompanyDetail(eq(OWN_COMPANY_NO), anyString());
    }

    @Test
    @DisplayName("일반 사용자가 다른 회사 상세를 조회하면 403")
    void userCannotReadOtherCompany() throws Exception {
        given(authorizationService.getCurrentRole()).willReturn(Role.USER);
        given(authorizationService.getCompanyNo()).willReturn(OWN_COMPANY_NO);

        mockMvc.perform(get(DETAIL_PATH, OTHER_COMPANY_NO))
                .andExpect(status().isForbidden());

        then(miCompanyService).should(never()).getCompanyDetail(anyInt(), anyString());
    }

    @Test
    @DisplayName("운영자는 자기 회사 상세를 조회할 수 있다")
    void operatorCanReadOwnCompany() throws Exception {
        given(authorizationService.getCurrentRole()).willReturn(Role.OPERATOR);
        given(authorizationService.getCompanyNo()).willReturn(OWN_COMPANY_NO);

        mockMvc.perform(get(DETAIL_PATH, OWN_COMPANY_NO))
                .andExpect(status().isOk());

        then(miCompanyService).should().getCompanyDetail(eq(OWN_COMPANY_NO), anyString());
    }

    @Test
    @DisplayName("운영자가 다른 회사 상세를 조회하면 403")
    void operatorCannotReadOtherCompany() throws Exception {
        given(authorizationService.getCurrentRole()).willReturn(Role.OPERATOR);
        given(authorizationService.getCompanyNo()).willReturn(OWN_COMPANY_NO);

        mockMvc.perform(get(DETAIL_PATH, OTHER_COMPANY_NO))
                .andExpect(status().isForbidden());

        then(miCompanyService).should(never()).getCompanyDetail(anyInt(), anyString());
    }

    @Test
    @DisplayName("관리자는 다른 회사 상세를 조회할 수 있다 (타사 접속 조건은 PermissionAspect 가 검사)")
    void adminCanReadOtherCompany() throws Exception {
        given(authorizationService.getCurrentRole()).willReturn(Role.ADMIN);

        mockMvc.perform(get(DETAIL_PATH, OTHER_COMPANY_NO))
                .andExpect(status().isOk());

        then(miCompanyService).should().getCompanyDetail(eq(OTHER_COMPANY_NO), anyString());
    }
}
