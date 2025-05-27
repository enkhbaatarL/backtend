package com.sk.domain.shared.repository.master.tmm;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sk.domain.shared.entity.compositeKey.tmm.CorrelationAnalysisResultId;
import com.sk.domain.shared.entity.master.tmm.CorrelationAnalysisResult;

public interface CorrelationAnalysisResultRepository extends JpaRepository<CorrelationAnalysisResult, CorrelationAnalysisResultId> {

}
