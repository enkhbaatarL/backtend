package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.GroupModelExecutionReq;
import com.sk.domain.mi.dto.response.nextgen.GroupModelExecutionRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamCompareRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamProposalsRes;

public interface ModelParamProposalGroupService {

    GroupModelParamProposalsRes getGroupProposals(Long scenarioGroupId);

    GroupModelParamCompareRes getGroupCompare(Long scenarioGroupId);

    GroupModelExecutionRes executeGroup(Long scenarioGroupId, GroupModelExecutionReq request);

}
