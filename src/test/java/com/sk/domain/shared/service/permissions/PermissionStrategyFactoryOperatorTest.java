package com.sk.domain.shared.service.permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PermissionStrategyFactory 운영자 규칙 평가 테스트")
@ExtendWith(MockitoExtension.class)
class PermissionStrategyFactoryOperatorTest {

    @Mock
    private AuthorizationService authService;

    private PermissionStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PermissionStrategyFactory(authService);
    }

    @Test
    @DisplayName("타사접속 가능 운영자는 USER 규칙으로 접근 가능 (조건 없음)")
    void serviceOperatorMatchesUserRule() throws Exception {
        given(authService.getCurrentRole()).willReturn(Role.OPERATOR);
        given(authService.isServiceAccessOperator()).willReturn(true);

        assertThat(factory.canAccessApi(rulesOf("userOrAdmin"))).isTrue();
    }

    @Test
    @DisplayName("타사접속 불가 운영자는 USER 규칙으로 접근 불가")
    void nonServiceOperatorDoesNotMatchUserRule() throws Exception {
        given(authService.getCurrentRole()).willReturn(Role.OPERATOR);
        given(authService.isServiceAccessOperator()).willReturn(false);

        assertThat(factory.canAccessApi(rulesOf("userOrAdmin"))).isFalse();
    }

    @Test
    @DisplayName("USER 규칙의 SK가스 조건은 운영자에게도 똑같이 적용")
    void skgasConditionAppliedToOperator() throws Exception {
        given(authService.getCurrentRole()).willReturn(Role.OPERATOR);
        given(authService.isServiceAccessOperator()).willReturn(true);
        given(authService.getCompanyCd()).willReturn("OTHER");

        assertThat(factory.canAccessApi(rulesOf("skgasUserOrAdmin"))).isFalse();

        given(authService.getCompanyCd()).willReturn(PermissionCondition.AllowedCompany.SKGAS.getCode());

        assertThat(factory.canAccessApi(rulesOf("skgasUserOrAdmin"))).isTrue();
    }

    @Test
    @DisplayName("ADMIN 규칙만 있는 API 는 운영자 접근 불가")
    void operatorCannotMatchAdminOnlyRule() throws Exception {
        given(authService.getCurrentRole()).willReturn(Role.OPERATOR);
        given(authService.isServiceAccessOperator()).willReturn(true);

        assertThat(factory.canAccessApi(rulesOf("adminOnly"))).isFalse();
    }

    @Test
    @DisplayName("OPERATOR 규칙이 있으면 타사접속 여부를 보지 않고 통과")
    void operatorRuleMatchesDirectly() throws Exception {
        given(authService.getCurrentRole()).willReturn(Role.OPERATOR);

        assertThat(factory.canAccessApi(rulesOf("operatorOrAdmin"))).isTrue();
        verify(authService, never()).isServiceAccessOperator();
    }

    private static PermissionRule[] rulesOf(String methodName) throws NoSuchMethodException {
        return RuleFixtures.class.getDeclaredMethod(methodName).getAnnotation(RequiredPermissionRules.class).rules();
    }

    @SuppressWarnings("unused")
    private static class RuleFixtures {

        @RequiredPermissionRules(rules = {
            @PermissionRule(role = Role.USER),
            @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
        })
        void userOrAdmin() {}

        @RequiredPermissionRules(rules = {
            @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
            @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
        })
        void skgasUserOrAdmin() {}

        @RequiredPermissionRules(rules = {
            @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
        })
        void adminOnly() {}

        @RequiredPermissionRules(rules = {
            @PermissionRule(role = Role.OPERATOR),
            @PermissionRule(role = Role.ADMIN)
        })
        void operatorOrAdmin() {}
    }
}
