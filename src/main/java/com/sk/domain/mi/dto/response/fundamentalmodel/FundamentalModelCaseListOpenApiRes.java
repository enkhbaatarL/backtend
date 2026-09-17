package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.common.model.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BaseResponseFundamentalModelCaseListRes")
public final class FundamentalModelCaseListOpenApiRes extends BaseResponse<FundamentalModelCaseListRes> {

    private FundamentalModelCaseListOpenApiRes() {
        super("", "");
    }
}
