package com.sk.common.interceptor;

//TODO: 삭제 예정 => Datasource 설정은 mybatis 호출 직전에
/**
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    private final CustomerCompanyRepository customerCompanyRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication instanceof JwtAuthenticationToken jwtToken) {
                String companyNo = jwtToken.getToken().getClaimAsString("companyNo");
                if (companyNo != null) {
                    try {
                        CustomerCompany customerCompany = customerCompanyRepository
                                .findByCustomerCompanyNoAndIsDeletedFalse(Integer.parseInt(companyNo))
                                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_DATA_NOT_FOUND,
                                        String.format("Company Information not found. companyNo: %s", companyNo)));
                        log.info("Set tenant datasource. companyCd: {}", customerCompany.getCustomerCompanyCd());
                        DataSourceContextHolder.setDataSourceKey(customerCompany.getCustomerCompanyCd());
                    } catch (Exception e) {
                        throw AppErrorException.of(ResultCode.Error.SYS_UNKNOWN,
                                String.format("Failed to set tenant datasource. companyNo: %s", companyNo));
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        DataSourceContextHolder.clearDataSourceKey();
    }
}
 */
