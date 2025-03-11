package com.sk.domain.admin.dto.request;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

public record CompanyInfoReq(

        @NotEmpty String companyCd,

        @NotEmpty String companyNm,

        String companyEngNm

) {
}
