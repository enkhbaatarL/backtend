package com.sk.domain.mi.dto.response.newsletter;

public record RevisionFlowGuideRes(
    String scenario,
    Boolean hasRevisionFallback,
    Endpoints endpoints
) {

    public record Endpoints(
        Draft draft,
        Save save,
        DraftLoad draftLoad,
        History history,
        SendMail sendMail
    ) {
    }

    public record Draft(
        String request,
        Boolean createIfMissing,
        String conflictRule
    ) {
    }

    public record Save(
        String request,
        Boolean createIfMissing,
        String transitionTo
    ) {
    }

    public record DraftLoad(
        String request,
        String note
    ) {
    }

    public record History(
        String list,
        String detail
    ) {
    }

    public record SendMail(
        String request,
        String guard
    ) {
    }
}
