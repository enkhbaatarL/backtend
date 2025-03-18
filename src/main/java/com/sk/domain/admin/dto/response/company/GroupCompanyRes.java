package com.sk.domain.admin.dto.response.company;

import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Comparator;
import java.util.List;

public record GroupCompanyRes(

    @Schema(description = "회사코드", example = "SK001")
    String companyCd,

    @Schema(description = "회사명", example = "sk이노베이션")
    String companyNm,

    @Schema(description = "회사영문명", example = "skinnovation")
    String companyEngNm

) {

    @Builder
    public GroupCompanyRes(String companyCd, String companyNm, String companyEngNm) {
        this.companyCd = companyCd;
        this.companyNm = companyNm;
        this.companyEngNm = companyEngNm;
    }

    public static GroupCompanyRes of(CompanyInfo companyInfo) {
        return GroupCompanyRes.builder()
            .companyCd(companyInfo.getCompanyCd())
            .companyNm(companyInfo.getCompanyNm())
            .companyEngNm(companyInfo.getCompanyEngNm())
            .build();
    }

    public static List<GroupCompanyRes> ofListOrderByAsc(List<CompanyInfo> companies, Comparator<CompanyInfo> comparator) {
        return companies.stream()
            .sorted(comparator)
            .map(GroupCompanyRes::of)
            .toList();
    }

}
