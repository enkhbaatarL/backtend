package com.sk.domain.mi.service.fundamentalmodel.mock;

import com.sk.domain.mi.config.FundamentalModelMockProperties;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunAcceptRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunCallbackRes;
import com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseService;
import com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseServiceImpl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
@RequiredArgsConstructor
public class FundamentalModelCaseServiceDelegator implements FundamentalModelCaseService {

    private final FundamentalModelCaseServiceImpl realService;
    private final FundamentalModelCaseMockService mockService;
    private final FundamentalModelMockProperties mockProps;

    @Override
    public FundamentalModelCaseListRes listCases(FundamentalModelCaseListReq request) {
        return mockProps.getFundamentalModelCases().getApi().isListCases()
            ? mockService.listCases(request)
            : realService.listCases(request);
    }

    @Override
    public FundamentalModelCaseCreateRes createCase(
        String title,
        String description,
        List<MultipartFile> files
    ) {
        return mockProps.getFundamentalModelCases().getApi().isCreateCase()
            ? mockService.createCase(title, description, files)
            : realService.createCase(title, description, files);
    }

    @Override
    public FundamentalModelRunAcceptRes runCase(Long caseId, FundamentalModelRunReq request) {
        return mockProps.getFundamentalModelCases().getApi().isRun()
            ? mockService.runCase(caseId, request)
            : realService.runCase(caseId, request);
    }

    @Override
    public FundamentalModelCaseStatusRes getCaseStatus(Long caseId) {
        return mockProps.getFundamentalModelCases().getApi().isGetStatus()
            ? mockService.getCaseStatus(caseId)
            : realService.getCaseStatus(caseId);
    }

    @Override
    public FundamentalModelRunCallbackRes handleCallback(String runId, FundamentalModelRunCallbackReq request) {
        return mockProps.getFundamentalModelRuns().getApi().isCallback()
            ? mockService.handleCallback(runId, request)
            : realService.handleCallback(runId, request);
    }

    @Override
    public void reset() {
        mockService.reset();
    }
}
