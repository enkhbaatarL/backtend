package com.sk.domain.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.entity.master.tmd.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CompanyOverviewRes(

    CompanyRes company,
    List<ProviderRes> providers,
    List<UserRes> users

) {

    @Builder
    public CompanyOverviewRes(CompanyRes company, List<ProviderRes> providers, List<UserRes> users) {
        this.company = company;
        this.providers = providers;
        this.users = users;
    }

    public record ProviderRes(

        @Schema(description = "프로바이더번호", example = "")
        Integer providerNo,

        @Schema(description = "프로바이더명", example = "")
        String providerNm,

        @Schema(description = "프로바이더코드", example = "")
        String providerCd,

        @Schema(description = "회사에 등록된 상품 개수", example = "")
        int numberOfCompanyGoods,

        @JsonUnwrapped
        @Schema(description = "감사 정보")
        AuditInfo auditInfo

    ) {

        @Builder
        public ProviderRes(Integer providerNo, String providerNm, String providerCd, int numberOfCompanyGoods, AuditInfo auditInfo) {
            this.providerNo = providerNo;
            this.providerNm = providerNm;
            this.providerCd = providerCd;
            this.numberOfCompanyGoods = numberOfCompanyGoods;
            this.auditInfo = auditInfo;
        }

        public static ProviderRes of(CustomerCompanyProviderMapping providerMapping, Map<Integer, Long> groupedProviderNo) {
            Provider provider = providerMapping.getProvider();
            return ProviderRes.builder()
                .providerNo(provider.getProviderNo())
                .providerNm(provider.getProviderNm())
                .providerCd(provider.getProviderCd())
                .numberOfCompanyGoods(groupedProviderNo.getOrDefault(provider.getProviderNo(), 0L).intValue())
                .auditInfo(AuditInfo.of(provider.getCreateUserId(), provider.getCreateDt(), provider.getModifyUserId(), provider.getModifyDt()))
                .build();
        }

        public static List<ProviderRes> ofList(List<CustomerCompanyProviderMapping> providerMappings, Map<Integer, Long> groupedProviderNo) {
            return providerMappings.stream().map(providerMapping -> of(providerMapping, groupedProviderNo)).toList();
        }

    }

    public record UserRes(

        @Schema(description = "사용자번호", example = "")
        Integer userNo,

        @Schema(description = "사용자명", example = "")
        String userNm,

        @Schema(description = "사용자로그인ID", example = "")
        String userLoginId,

        @Schema(description = "사용자역할코드", example = "")
        String userRoleCd,

        @Schema(description = "직책명", example = "")
        String occupationDesignationNm,

        @Schema(description = "부서명", example = "")
        String departmentNm,

        @JsonUnwrapped
        @Schema(description = "감사 정보")
        AuditInfo auditInfo

    ) {

        @Builder
        public UserRes(Integer userNo, String userNm, String userLoginId, String userRoleCd, String occupationDesignationNm, String departmentNm, AuditInfo auditInfo) {
            this.userNo = userNo;
            this.userNm = userNm;
            this.userLoginId = userLoginId;
            this.userRoleCd = userRoleCd;
            this.occupationDesignationNm = occupationDesignationNm;
            this.departmentNm = departmentNm;
            this.auditInfo = auditInfo;
        }

        public static UserRes of(User user) {
            return UserRes.builder()
                .userNo(user.getUserNo())
                .userNm(user.getUserNm())
                .userLoginId(user.getUserLoginId())
                .userRoleCd(user.getUserRoleCd().getCode())
                .occupationDesignationNm(user.getOccupationDesignationNm())
                .departmentNm(user.getDepartmentNm())
                .auditInfo(AuditInfo.of(user.getCreateUserId(), user.getCreateDt(), user.getModifyUserId(), user.getModifyDt()))
                .build();
        }

        public static List<UserRes> ofList(List<User> users) {
            return users.stream().map(UserRes::of).toList();
        }
    }

    public static CompanyOverviewRes of(CustomerCompany company, List<User> users, List<CustomerCompanyProviderMapping> providerMappings, List<CustomerCompanyGoodsMapping> goodsMappings) {

        Map<Integer, Long> groupedProviderNo = goodsMappings.stream()
            .collect(Collectors.groupingBy(goodsMapping -> goodsMapping.getGoods().getProvider().getProviderNo(), Collectors.counting()));

        return CompanyOverviewRes.builder()
            .company(CompanyRes.of(company))
            .providers(ProviderRes.ofList(providerMappings, groupedProviderNo))
            .users(UserRes.ofList(users))
            .build();
    }

}
