package com.sk.domain.shared.service.permissions.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("OperatorPermissionStrategy 테스트")
@ExtendWith(MockitoExtension.class)
class OperatorPermissionStrategyTest {

    private static final int OWN_COMPANY_NO = 1;
    private static final int GOODS_NO = 100;

    @Mock
    private AuthorizationService authService;

    private OperatorPermissionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new OperatorPermissionStrategy(authService);
    }

    @Test
    @DisplayName("OPERATOR 를 허용한 API 는 타사접속 여부와 무관하게 접근 가능")
    void operatorRoleAllowed() {
        boolean result = strategy.canAccessApi(new Role[] {Role.ADMIN, Role.OPERATOR}, PermissionCondition.NONE);

        assertThat(result).isTrue();
        verify(authService, never()).isServiceAccessOperator();
    }

    @Test
    @DisplayName("타사접속 가능 운영자는 USER 허용 API 에 접근 가능")
    void serviceOperatorCanAccessUserApi() {
        given(authService.isServiceAccessOperator()).willReturn(true);

        assertThat(strategy.canAccessApi(new Role[] {Role.ADMIN, Role.USER}, PermissionCondition.THIRD_PARTY_ADMIN_ONLY))
                .isTrue();
    }

    @Test
    @DisplayName("타사접속 불가 운영자는 USER 허용 API 에 접근 불가")
    void nonServiceOperatorCannotAccessUserApi() {
        given(authService.isServiceAccessOperator()).willReturn(false);

        assertThat(strategy.canAccessApi(new Role[] {Role.ADMIN, Role.USER}, PermissionCondition.NONE)).isFalse();
    }

    @Test
    @DisplayName("타사접속 가능 운영자라도 USER 를 허용하지 않은 API 는 접근 불가")
    void serviceOperatorCannotAccessAdminOnlyApi() {
        given(authService.isServiceAccessOperator()).willReturn(true);

        assertThat(strategy.canAccessApi(new Role[] {Role.ADMIN}, PermissionCondition.NONE)).isFalse();
    }

    @Test
    @DisplayName("SK가스 사용자 한정 API 는 운영자 회사가 SK가스일 때만 접근 가능")
    void skgasConditionAppliedToOperator() {
        given(authService.isServiceAccessOperator()).willReturn(true);
        given(authService.getCompanyCd()).willReturn("OTHER");

        assertThat(strategy.canAccessApi(new Role[] {Role.USER}, PermissionCondition.SKGAS_COMPANY_USER_ONLY))
                .isFalse();

        given(authService.getCompanyCd()).willReturn(PermissionCondition.AllowedCompany.SKGAS.getCode());

        assertThat(strategy.canAccessApi(new Role[] {Role.USER}, PermissionCondition.SKGAS_COMPANY_USER_ONLY))
                .isTrue();
    }

    @Test
    @DisplayName("타사접속 가능 운영자의 상품 권한은 자기 회사(token claim) 기준으로 판정")
    void serviceOperatorGoodsPermissionByOwnCompany() {
        PermissionRes permissions = mock(PermissionRes.class);
        given(authService.isServiceAccessOperator()).willReturn(true);
        given(authService.getPermissions()).willReturn(permissions);
        given(authService.getCompanyNo()).willReturn(OWN_COMPANY_NO);
        given(permissions.getGoodsViewableMap(OWN_COMPANY_NO)).willReturn(Map.of(GOODS_NO, true));
        given(permissions.getGoodsDownloadableMap(OWN_COMPANY_NO)).willReturn(Map.of(GOODS_NO, false));

        assertThat(strategy.canViewGoods(GOODS_NO)).isTrue();
        assertThat(strategy.canDownloadGoods(GOODS_NO)).isFalse();
        verify(authService, never()).getCompanyNoWithPriority();
    }

    @Test
    @DisplayName("타사접속 불가 운영자는 상품 조회·다운로드 불가 (기존 동작 유지)")
    void nonServiceOperatorHasNoGoodsPermission() {
        given(authService.isServiceAccessOperator()).willReturn(false);

        assertThat(strategy.canViewGoods(GOODS_NO)).isFalse();
        assertThat(strategy.canDownloadGoods(GOODS_NO)).isFalse();
        verify(authService, never()).getPermissions();
    }
}
