package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.DesignEventListReq;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGenCallbackReq;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGenReq;
import com.sk.domain.mi.dto.response.nextgen.DesignEventListRes;
import com.sk.domain.mi.dto.response.nextgen.GeneratedScenarioRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGenAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGenStatusRes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DesignServiceImpl implements DesignService {

    @Override
    public DesignEventListRes getDesignEvents(DesignEventListReq request) {
        throw new UnsupportedOperationException("Design 실 DB 연동 미구현");
    }

    @Override
    public ScenarioGenAcceptRes generateScenarios(ScenarioGenReq request) {
        throw new UnsupportedOperationException("Design 실 DB 연동 미구현");
    }

    @Override
    public void handleCallback(ScenarioGenCallbackReq request) {
        throw new UnsupportedOperationException("Design 실 DB 연동 미구현");
    }

    @Override
    public List<GeneratedScenarioRes> getGeneratedScenarios(Long requestId) {
        throw new UnsupportedOperationException("Design 실 DB 연동 미구현");
    }

    @Override
    public List<ScenarioGenStatusRes> getGenerateStatuses(List<Long> requestIds) {
        throw new UnsupportedOperationException("Design 실 DB 연동 미구현");
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Design 실 DB 연동 미구현");
    }

}
