package com.sk.domain.shared.repository.master.tmd.projection;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;

public interface ModelExecutionRunStateRow {

    String getRunId();

    FundamentalModelRunStatus getStatus();

    String getFailureReasonCode();
}
