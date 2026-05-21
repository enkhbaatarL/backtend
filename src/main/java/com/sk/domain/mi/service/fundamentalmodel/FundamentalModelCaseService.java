package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunAcceptRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunCallbackRes;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FundamentalModelCaseService {

    FundamentalModelCaseListRes listCases(FundamentalModelCaseListReq request);

    FundamentalModelCaseCreateRes createCase(
        String title,
        String description,
        List<MultipartFile> files
    );

    FundamentalModelRunAcceptRes runCase(Long caseId, FundamentalModelRunReq request);

    FundamentalModelCaseStatusRes getCaseStatus(Long caseId);

    FundamentalModelRunCallbackRes handleCallback(String runId, FundamentalModelRunCallbackReq request);

    void reset();
}
