package com.sk.domain.mi.service.fundamentalmodel.mock;

import com.sk.domain.mi.config.FundamentalModelMockProperties;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseUpdateReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelVolumesReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseCreateRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseDetailRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseListRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelCaseStatusRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelPriceRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunAcceptRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunCallbackRes;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelVolumesRes;
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
    public FundamentalModelCaseDetailRes getCaseDetail(Long caseId) {
        return mockProps.getFundamentalModelCases().getApi().isGetDetail()
            ? mockService.getCaseDetail(caseId)
            : realService.getCaseDetail(caseId);
    }

    @Override
    public FundamentalModelCaseDetailRes updateCase(Long caseId, FundamentalModelCaseUpdateReq request) {
        return mockProps.getFundamentalModelCases().getApi().isUpdate()
            ? mockService.updateCase(caseId, request)
            : realService.updateCase(caseId, request);
    }

    @Override
    public void deleteCase(Long caseId) {
        if (mockProps.getFundamentalModelCases().getApi().isDelete()) {
            mockService.deleteCase(caseId);
        } else {
            realService.deleteCase(caseId);
        }
    }

    @Override
    public FundamentalModelRunAcceptRes runCase(Long caseId) {
        return mockProps.getFundamentalModelCases().getApi().isRun()
            ? mockService.runCase(caseId)
            : realService.runCase(caseId);
    }

    @Override
    public FundamentalModelCaseStatusRes getCaseStatus(Long caseId) {
        return mockProps.getFundamentalModelCases().getApi().isGetStatus()
            ? mockService.getCaseStatus(caseId)
            : realService.getCaseStatus(caseId);
    }

    @Override
    public FundamentalModelVolumesRes getVolumes(Long caseId, FundamentalModelVolumesReq request) {
        return mockProps.getFundamentalModelCases().getApi().isGetVolumes()
            ? mockService.getVolumes(caseId, request)
            : realService.getVolumes(caseId, request);
    }

    @Override
    public FundamentalModelPriceRes getPrice(Long caseId) {
        return mockProps.getFundamentalModelCases().getApi().isGetPrice()
            ? mockService.getPrice(caseId)
            : realService.getPrice(caseId);
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
