package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.AgentModelParamProposal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentModelParamProposalRepository extends JpaRepository<AgentModelParamProposal, Long> {

    Optional<AgentModelParamProposal> findByModelParamProposalIdAndScenarioIdAndIsDeletedFalse(
        Long modelParamProposalId,
        Long scenarioId
    );
}
