package com.sk.domain.mi.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.enums.GoodsAuthCd;
import com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public record PermissionRes(

    @Schema(description = "권한주체구분코드", example = "INDIVIDUAL", allowableValues = {"COMPANY", "INDIVIDUAL"})
    String goodsAuthManagementSeparateCd,

    @Schema(description = "권한주체구분코드", example = "개인", allowableValues = {"회사", "개인"})
    String goodsAuthManagementSeparateNm,

    @Schema(description = "회사 권한 목록")
    List<CompanyAuthRes> companies

) {

    @Builder
    public PermissionRes(String goodsAuthManagementSeparateCd, String goodsAuthManagementSeparateNm, List<CompanyAuthRes> companies) {
        this.goodsAuthManagementSeparateCd = goodsAuthManagementSeparateCd;
        this.goodsAuthManagementSeparateNm = goodsAuthManagementSeparateNm;
        this.companies = companies;
    }

    public record CompanyAuthRes(

        @Schema(description = "회사 번호", example = "159")
        Integer companyNo,

        @Schema(description = "회사 명", example = "SK이노베이션")
        String companyNm,

        @Schema(description = "회사 > 프로바이더 권한 목록")
        List<ProviderAuthRes> providers

    ) {
        @Builder
        public CompanyAuthRes(Integer companyNo, String companyNm, List<ProviderAuthRes> providers) {
            this.companyNo = companyNo;
            this.companyNm = companyNm;
            this.providers = providers;
        }
    }

    public record ProviderAuthRes(

        @Schema(description = "프로바이더 번호", example = "42")
        Integer providerNo,

        @Schema(description = "프로바이더 명", example = "LSEG")
        String providerNm,

        @Schema(description = "회사 > 프로바이더 > 상품 권한 목록")
        List<GoodsAuthRes> goods

    ) {
        @Builder
        public ProviderAuthRes(Integer providerNo, String providerNm, List<GoodsAuthRes> goods) {
            this.providerNo = providerNo;
            this.providerNm = providerNm;
            this.goods = goods;
        }
    }

    public record GoodsAuthRes(

        @Schema(description = "상품번호", example = "12")
        Integer goodsNo,

        @Schema(description = "상품ID", example = "CD159")
        String goodsId,

        @Schema(description = "상품명", example = "제조사별 DRAM 생산량")
        String goodsNm,

        @Schema(description = "보기권한 여부", example = "true")
        boolean isViewable,

        @Schema(description = "다운로드권한 여부", example = "false")
        boolean isDownloadable

    ) {
        @Builder
        public GoodsAuthRes(Integer goodsNo, String goodsId, String goodsNm, boolean isViewable, boolean isDownloadable) {
            this.goodsNo = goodsNo;
            this.goodsId = goodsId;
            this.goodsNm = goodsNm;
            this.isViewable = isViewable;
            this.isDownloadable = isDownloadable;
        }
    }

    public static PermissionRes fromAuthorityMappings(List<AuthManageGoodsMapping> goodsMappings) {

        AtomicReference<GoodsAuthManagementSeparateCd> goodsAuthManagementSeparateCd = new AtomicReference<>();

        // 1. 회사별로 그룹화
        Map<CustomerCompany, List<AuthManageGoodsMapping>> byCompany = goodsMappings.stream()
            .collect(Collectors.groupingBy(mapping -> mapping.getCustomerCompanyGoodsMapping().getCustomerCompany()));

        // 2. 회사별 CompanyAuth 생성
        List<CompanyAuthRes> companyAuthRes = byCompany.entrySet().stream()
            .map(entry -> {
                CustomerCompany company = entry.getKey();
                List<AuthManageGoodsMapping> companyMappings = entry.getValue();

                // 3. 프로바이더별로 그룹화
                Map<Provider, List<AuthManageGoodsMapping>> byProvider = companyMappings.stream()
                    .collect(Collectors.groupingBy(mapping -> mapping.getCustomerCompanyGoodsMapping().getGoods().getProvider()));

                // 4. 프로바이더별 ProviderAuth 생성
                List<ProviderAuthRes> providerAuthRes = byProvider.entrySet().stream()
                    .map(providerEntry -> {
                        Provider provider = providerEntry.getKey();
                        List<AuthManageGoodsMapping> providerMappings = providerEntry.getValue();

                        // 5. 상품별로 그룹화
                        Map<Integer, List<AuthManageGoodsMapping>> byGoods = providerMappings.stream()
                            .collect(Collectors.groupingBy(mapping -> mapping.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo()));

                        // 6. 상품별 GoodsAuth 생성
                        List<GoodsAuthRes> goodsAuthRes = byGoods.entrySet().stream()
                            .map(goodsEntry -> {
                                Integer goodsNo = goodsEntry.getKey();
                                List<AuthManageGoodsMapping> goodsMappingList = goodsEntry.getValue();

                                // 첫 번째 매핑에서 공통 정보 추출
                                String goodsId = goodsMappingList.get(0).getCustomerCompanyGoodsMapping().getGoods().getGoodsId();
                                String goodsNm = goodsMappingList.get(0).getCustomerCompanyGoodsMapping().getGoods().getGoodsNm();
                                goodsAuthManagementSeparateCd.set(goodsMappingList.get(0).getCustomerCompanyGoodsMapping().getId()
                                    .getGoodsAuthManagementSeparateCd());

                                // 권한 설정 확인
                                boolean isView = false;
                                boolean isDownload = false;

                                for (AuthManageGoodsMapping mapping : goodsMappingList) {
                                    GoodsAuthCd authCd = mapping.getGoodsAuthCd();
                                    if (authCd == GoodsAuthCd.VIEW)
                                        isView = true;
                                    if (authCd == GoodsAuthCd.DOWNLOAD)
                                        isDownload = true;
                                    if (isView && isDownload)
                                        break;
                                }

                                return GoodsAuthRes.builder()
                                    .goodsNo(goodsNo)
                                    .goodsId(goodsId)
                                    .goodsNm(goodsNm)
                                    .isViewable(isView)
                                    .isDownloadable(isDownload)
                                    .build();
                            })
                            .sorted(Comparator.comparing(GoodsAuthRes::goodsNm))
                            .collect(Collectors.toList());

                        return ProviderAuthRes.builder()
                            .providerNo(provider.getProviderNo())
                            .providerNm(provider.getProviderNm())
                            .goods(goodsAuthRes)
                            .build();
                    })
                    .sorted(Comparator.comparing(ProviderAuthRes::providerNm))
                    .collect(Collectors.toList());

                return CompanyAuthRes.builder()
                    .companyNo(company.getCustomerCompanyNo())
                    .companyNm(company.getCustomerCompanyNm())
                    .providers(providerAuthRes)
                    .build();
            })
            .sorted(Comparator.comparing(CompanyAuthRes::companyNm))
            .collect(Collectors.toList());

        return new PermissionRes(
            Optional.ofNullable(goodsAuthManagementSeparateCd.get()).map(GoodsAuthManagementSeparateCd::getCode).orElse(null),
            Optional.ofNullable(goodsAuthManagementSeparateCd.get()).map(GoodsAuthManagementSeparateCd::getDescription).orElse(null),
            companyAuthRes);
    }

    @JsonIgnore
    public List<Integer> getPermittedCompanies() {
        return this.companies.stream()
            .map(obj -> obj.companyNo)
            .toList();
    }

    @JsonIgnore
    public List<Integer> getPermittedProviders() {
        return this.companies.stream()
            .flatMap(obj -> obj.providers.stream()
                .map(obj2 -> obj2.providerNo))
            .toList();
    }

    @JsonIgnore
    public List<Integer> getPermittedGoods() {
        return this.companies.stream()
            .flatMap(obj -> obj.providers.stream()
                .flatMap(obj2 -> obj2.goods.stream()))
            .map(obj3 -> obj3.goodsNo)
            .toList();
    }

    @JsonIgnore
    public Map<Integer, Boolean> getGoodsViewableMap() {

        return this.companies.stream()
            .flatMap(obj -> obj.providers.stream()
                .flatMap(obj2 -> obj2.goods.stream()))
            .collect(Collectors.toMap(
                GoodsAuthRes::goodsNo,
                GoodsAuthRes::isViewable));

    }

    @JsonIgnore
    public Map<Integer, Boolean> getGoodsDownloadableMap() {

        return this.companies.stream()
            .flatMap(obj -> obj.providers.stream()
                .flatMap(obj2 -> obj2.goods.stream()))
            .collect(Collectors.toMap(
                GoodsAuthRes::goodsNo,
                GoodsAuthRes::isDownloadable));

    }

}
