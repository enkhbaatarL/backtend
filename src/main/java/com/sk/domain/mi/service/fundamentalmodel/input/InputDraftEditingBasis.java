package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** transaction에서 확인한 고정 원본과 성공 이력. 파일 IO 동안 DB 잠금/인증 ThreadLocal을 사용하지 않는다. */
record InputDraftEditingBasis(Draft draft, ModelInputDraftService.Source source, List<Revision> revisions,
        List<RowIdentity> identities, List<RowPosition> positions) {
    InputDraftEditingBasis {
        revisions = List.copyOf(revisions); identities = List.copyOf(identities); positions = List.copyOf(positions);
    }

    InputDraftHistory history(long revision, InputDraftJson json) {
        if (revision < 0 || revision > draft.revision()) throw InputDraftException.internal();
        List<Revision> prefix = revisions.stream().filter(r -> r.number() <= revision).toList();
        Set<String> operations = prefix.stream().filter(r -> r.operation() != null).map(Revision::operation).collect(Collectors.toSet());
        List<RowIdentity> rows = identities.stream().filter(row -> row.baseline() <= revision
            && (row.original() != null || operations.contains(row.operation()))).toList();
        Set<String> ids = rows.stream().map(RowIdentity::id).collect(Collectors.toSet());
        List<RowPosition> locations = positions.stream().filter(p -> ids.contains(p.id()) && p.begin() <= revision)
            .map(p -> new RowPosition(p.table(), p.id(), p.begin(), p.end() != null && p.end() <= revision ? p.end() : null, p.row())).toList();
        return InputDraftHistory.restore(draft.atRevision(revision), source.catalog(), source.definitions(), prefix, rows, locations, json);
    }
}
