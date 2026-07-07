package com.sk.domain.shared.enums.nextgen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ModelInputMode eligibility 판정 테스트")
class ModelInputModeTest {

    @DisplayName("미대상 집합(3,12,13,14,15,16,20)은 FILE_UPLOAD, 제안 대상 아님")
    @ParameterizedTest
    @ValueSource(longs = {3L, 12L, 13L, 14L, 15L, 16L, 20L})
    void fileUploadWhenExcluded(long eventTypeL2Id) {
        ModelInputMode mode = ModelInputMode.from(eventTypeL2Id);

        assertThat(mode).isEqualTo(ModelInputMode.FILE_UPLOAD);
        assertThat(mode.isParameterProposal()).isFalse();
    }

    @DisplayName("제외 목록에 없는 ID는 PARAMETER_PROPOSAL, 제안 대상")
    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 4L, 11L, 17L, 18L, 19L, 21L})
    void parameterProposalWhenNotExcluded(long eventTypeL2Id) {
        ModelInputMode mode = ModelInputMode.from(eventTypeL2Id);

        assertThat(mode).isEqualTo(ModelInputMode.PARAMETER_PROPOSAL);
        assertThat(mode.isParameterProposal()).isTrue();
    }

    @DisplayName("null eventTypeL2Id는 PARAMETER_PROPOSAL로 기본 판정")
    @ParameterizedTest
    @NullSource
    void parameterProposalWhenNull(Long eventTypeL2Id) {
        assertThat(ModelInputMode.from(eventTypeL2Id)).isEqualTo(ModelInputMode.PARAMETER_PROPOSAL);
    }

}
