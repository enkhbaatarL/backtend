package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseListReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelCaseUpdateReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelTradeFlowsReq;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelVolumesReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FilterOption;
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
import com.sk.domain.mi.dto.response.fundamentalmodel.VolumesSegmentFilterOptions;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelDataType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelGranularity;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelSegment;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowDirection;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FundamentalModelCaseServiceImpl implements FundamentalModelCaseService {

    @Override
    public FundamentalModelCaseListRes listCases(FundamentalModelCaseListReq request) {
        throw new UnsupportedOperationException("Fundamental model case list DB implementation is not ready.");
    }

    @Override
    public FundamentalModelCaseCreateRes createCase(
        String title,
        String description,
        List<MultipartFile> files
    ) {
        throw new UnsupportedOperationException("Fundamental model case create DB implementation is not ready.");
    }

    @Override
    public FundamentalModelCaseDetailRes getCaseDetail(Long caseId) {
        throw new UnsupportedOperationException("Fundamental model case detail DB implementation is not ready.");
    }

    @Override
    public FundamentalModelCaseDetailRes updateCase(Long caseId, FundamentalModelCaseUpdateReq request) {
        throw new UnsupportedOperationException("Fundamental model case update DB implementation is not ready.");
    }

    @Override
    public void deleteCase(Long caseId) {
        throw new UnsupportedOperationException("Fundamental model case delete DB implementation is not ready.");
    }

    @Override
    public FundamentalModelRunAcceptRes runCase(Long caseId) {
        throw new UnsupportedOperationException("Fundamental model case run DB implementation is not ready.");
    }

    @Override
    public FundamentalModelCaseStatusRes getCaseStatus(Long caseId) {
        throw new UnsupportedOperationException("Fundamental model case status DB implementation is not ready.");
    }

    @Override
    public FundamentalModelVolumesFilterOptionsRes getVolumesFilterOptions() {
        List<FilterOption> allRegions = Arrays.stream(FundamentalModelRegion.values())
            .map(r -> new FilterOption(r.getCode(), r.getLabel()))
            .toList();
        List<FilterOption> granularities = Arrays.stream(FundamentalModelGranularity.values())
            .map(g -> new FilterOption(g.getCode(), g.getLabel()))
            .toList();

        VolumesSegmentFilterOptions global = new VolumesSegmentFilterOptions(
            FundamentalModelSegment.GLOBAL,
            Arrays.stream(FundamentalModelDataType.values())
                .map(t -> new FilterOption(t.getCode(), t.getLabel()))
                .toList(),
            allRegions,
            granularities
        );
        VolumesSegmentFilterOptions lng = new VolumesSegmentFilterOptions(
            FundamentalModelSegment.LNG,
            List.of(
                new FilterOption(FundamentalModelDataType.LNG_IMPORTS.getCode(), FundamentalModelDataType.LNG_IMPORTS.getLabel()),
                new FilterOption(FundamentalModelDataType.LNG_EXPORTS.getCode(), FundamentalModelDataType.LNG_EXPORTS.getLabel())
            ),
            allRegions,
            granularities
        );
        VolumesSegmentFilterOptions trade = new VolumesSegmentFilterOptions(
            FundamentalModelSegment.TRADE,
            Arrays.stream(TradeFlowDirection.values())
                .map(d -> new FilterOption(d.getCode(), d.getLabel()))
                .toList(),
            allRegions,
            List.of()
        );

        return new FundamentalModelVolumesFilterOptionsRes(List.of(global, lng, trade));
    }

    @Override
    public FundamentalModelVolumesRes getVolumes(Long caseId, FundamentalModelVolumesReq request) {
        throw new UnsupportedOperationException("Fundamental model volumes DB implementation is not ready.");
    }

    @Override
    public FundamentalModelTradeFlowsRes getTradeFlows(Long caseId, FundamentalModelTradeFlowsReq request) {
        throw new UnsupportedOperationException("Fundamental model trade flows DB implementation is not ready.");
    }

    @Override
    public FundamentalModelPriceRes getPrice(Long caseId) {
        throw new UnsupportedOperationException("Fundamental model price DB implementation is not ready.");
    }

    @Override
    public FundamentalModelRunCallbackRes handleCallback(String runId, FundamentalModelRunCallbackReq request) {
        throw new UnsupportedOperationException("Fundamental model run callback DB implementation is not ready.");
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Fundamental model case reset DB implementation is not ready.");
    }
}
