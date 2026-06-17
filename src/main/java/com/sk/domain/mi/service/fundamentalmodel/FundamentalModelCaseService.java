package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseUpdateReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelTradeFlowsReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelVolumesReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseDetailRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelPriceRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunAcceptRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunCallbackRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelTradeFlowsRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelVolumesFilterOptionsRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelVolumesRes;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FundamentalModelCaseService {

    FundamentalModelCaseListRes listCases(FundamentalModelCaseListReq request);

    FundamentalModelCaseCreateRes createCase(
        String title,
        String description,
        List<MultipartFile> files
    );

    FundamentalModelCaseDetailRes getCaseDetail(Long caseId);

    FundamentalModelCaseDetailRes updateCase(Long caseId, FundamentalModelCaseUpdateReq request);

    void deleteCase(Long caseId);

    FundamentalModelRunAcceptRes runCase(Long caseId);

    FundamentalModelCaseStatusRes getCaseStatus(Long caseId);

    FundamentalModelVolumesFilterOptionsRes getVolumesFilterOptions();

    FundamentalModelVolumesRes getVolumes(Long caseId, FundamentalModelVolumesReq request);

    FundamentalModelTradeFlowsRes getTradeFlows(Long caseId, FundamentalModelTradeFlowsReq request);

    FundamentalModelPriceRes getPrice(Long caseId);

    FundamentalModelRunCallbackRes handleCallback(String runId, FundamentalModelRunCallbackReq request);
}
