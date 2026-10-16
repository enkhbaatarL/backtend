package com.sk.domain.mi.service.nextgen.paramproposal;

import com.sk.domain.mi.service.nextgen.paramproposal.excel.ModelInputFile;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MI Base 기준 묶음(case 1건 + SCNRCASE input 4파일) 조회·검증.
 * 파라미터 제안 생성(Agent 요청)과 확정 실행 병합이 같은 규칙으로 기준본을 고르도록 공유한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiBaseInputSetResolver {

    private static final String FILE_RELATION_SCENARIO_CASE = "SCNRCASE";

    private final ScenarioCaseRepository scenarioCaseRepository;
    private final UploadedFileRepository uploadedFileRepository;

    /** current MI Base의 기준 묶음. MI Base가 없으면 empty, 완료 상태가 아니거나 input 4파일 조건이 맞지 않으면 예외. */
    public Optional<MiBaseInputSet> findCurrent() {
        return scenarioCaseRepository.findCurrentMiBase().map(miBase -> {
            if (miBase.getStatus() != FundamentalModelCaseStatus.COMPLETED) {
                throw new InvalidMiBaseInputSetException(miBase.getCaseId(), "case is not completed");
            }
            return resolve(miBase.getCaseId());
        });
    }

    /**
     * 지정 case의 active SCNRCASE input 4파일.
     * 제안 생성 때 고정한 case를 실행 시점에 다시 읽을 때도 쓰므로 current MI Base 여부와 case 상태는 보지 않는다.
     */
    public MiBaseInputSet resolve(Long caseId) {
        List<UploadedFile> inputFiles = uploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc(
            Math.toIntExact(caseId),
            FILE_RELATION_SCENARIO_CASE
        );
        if (inputFiles.size() != ModelInputFile.values().length) {
            throw new InvalidMiBaseInputSetException(caseId, "input file count must be exactly %d".formatted(ModelInputFile.values().length));
        }

        Map<ModelInputFile, InputFile> files = new EnumMap<>(ModelInputFile.class);
        for (UploadedFile inputFile : inputFiles) {
            String fileName = inputFile.getOriginalFileNm() == null ? "" : inputFile.getOriginalFileNm().toUpperCase(Locale.ROOT);
            List<ModelInputFile> matchedRoles = Arrays.stream(ModelInputFile.values())
                .filter(role -> fileName.contains(role.name()))
                .toList();
            if (matchedRoles.size() != 1 || inputFile.getStoragePathNm() == null || inputFile.getStoragePathNm().isBlank()) {
                log.error("[MI Base Input] input 파일 역할·경로 오류: caseId={}, fileNo={}, matchedRoles={}", caseId, inputFile.getFileNo(), matchedRoles);
                throw new InvalidMiBaseInputSetException(caseId, "input file role or storage path is invalid");
            }
            InputFile previous = files.put(matchedRoles.get(0), new InputFile(inputFile.getFileNo(), inputFile.getStoragePathNm()));
            if (previous != null) {
                throw new InvalidMiBaseInputSetException(caseId, "input file role is duplicated: " + matchedRoles.get(0).name());
            }
        }
        return new MiBaseInputSet(caseId, files);
    }

    /** files는 DEMAND/SUPPLY/TRADE/OUTPUT 역할 순서를 유지한다. */
    public record MiBaseInputSet(Long caseId, Map<ModelInputFile, InputFile> files) {

        public MiBaseInputSet {
            files = Collections.unmodifiableMap(new EnumMap<>(files));
        }
    }

    /** storagePath: tcm_filel 저장 경로 원문(URL). */
    public record InputFile(Long fileNo, String storagePath) {
    }

    @Getter
    public static final class InvalidMiBaseInputSetException extends RuntimeException {

        private final Long caseId;
        private final String reason;

        public InvalidMiBaseInputSetException(Long caseId, String reason) {
            super("MI Base input set is invalid. caseId: %d, reason: %s".formatted(caseId, reason));
            this.caseId = caseId;
            this.reason = reason;
        }
    }
}
