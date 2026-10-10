package com.sk.domain.mi.service.nextgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotListReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotLookupReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotRecordListReq;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotCompareRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotDetailRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotLookupRes;
import com.sk.domain.mi.dto.response.nextgen.snapshot.EventSnapshotRecordRes;
import com.sk.domain.shared.entity.master.tmm.MiEvent;
import com.sk.domain.shared.entity.master.tmm.MiEventSnapshot;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.nextgen.EventSnapshotCompareTarget;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventSnapshotInstitutionViewRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventSnapshotRecordMapRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventSnapshotRepository;
import com.sk.domain.shared.repository.master.tmm.projection.EventSnapshotRecordRow;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("이벤트 Snapshot 조회 서비스")
@ExtendWith(MockitoExtension.class)
class EventSnapshotServiceImplTest {

    private static final long EVENT_ID = 1269L;

    @Mock
    private MiEventSnapshotRepository snapshotRepository;

    @Mock
    private MiEventSnapshotRecordMapRepository recordMapRepository;

    @Mock
    private MiEventSnapshotInstitutionViewRepository institutionViewRepository;

    @Mock
    private MiEventRepository miEventRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private EventSnapshotServiceImpl service;

    @Test
    @DisplayName("다른 Event 의 snapshotId 로 상세를 조회하면 RSC-001")
    void rejectsSnapshotOfOtherEvent() {
        given(snapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(99L, EVENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEventSnapshot(EVENT_ID, 99L))
            .isInstanceOf(AppErrorException.class)
            .extracting("errorCode").isEqualTo(ResultCode.Error.RSC_NOT_FOUND);
    }

    @Test
    @DisplayName("저장 당시 Event 가 삭제된 Snapshot 도 정상 반환하고 시각은 UTC → KST 로 바꾼다")
    void returnsSnapshotOfDeletedEventInKst() {
        MiEventSnapshot snapshot = snapshot(12L, 4L, 11L).isEventDeleted(true)
            .createDt(LocalDateTime.of(2026, 9, 25, 8, 26, 6))
            .tagsJson("{\"tags\": [\"LNG\", \"JKM\"]}")
            .build();
        given(snapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(12L, EVENT_ID)).willReturn(Optional.of(snapshot));

        EventSnapshotDetailRes detail = service.getEventSnapshot(EVENT_ID, 12L);

        assertThat(detail.isEventDeleted()).isTrue();
        assertThat(detail.createdAt()).isEqualTo(LocalDateTime.of(2026, 9, 25, 17, 26, 6));
        assertThat(detail.keySummaries()).containsExactly("b1", "b2");
        assertThat(detail.tags()).containsExactly("LNG", "JKM");
        assertThat(detail.changedFields()).containsExactly("event_title");
    }

    @Test
    @DisplayName("목록의 날짜 필터는 KST 날짜를 UTC 범위로 바꿔 넘긴다 (종료일은 다음날 00:00 미만)")
    void convertsListDateRangeToUtc() {
        given(snapshotRepository.findEventSnapshots(eq(EVENT_ID), any(), any(), any(), any(), anyInt(), anyInt()))
            .willReturn(Page.empty());

        service.getEventSnapshots(EVENT_ID, new EventSnapshotListReq(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 15), null, " system ", null, null));

        then(snapshotRepository).should().findEventSnapshots(
            EVENT_ID,
            LocalDateTime.of(2026, 9, 14, 15, 0),
            LocalDateTime.of(2026, 9, 15, 15, 0),
            null,
            "system",
            1,
            20);
    }

    @Nested
    @DisplayName("버전·시점 조회")
    class Lookup {

        @Test
        @DisplayName("versionNo 와 asOf 를 같이 주면 REQ-003")
        void rejectsBothCriteria() {
            assertThatThrownBy(() -> service.lookupEventSnapshot(EVENT_ID, new EventSnapshotLookupReq(2L, LocalDateTime.now())))
                .isInstanceOf(AppErrorException.class)
                .extracting("errorCode").isEqualTo(ResultCode.Error.REQ_INVALID_PARAMETER);
        }

        @Test
        @DisplayName("파라미터가 없으면 최신 저장 버전")
        void returnsLatestWithoutCriteria() {
            given(snapshotRepository.findLatestEventSnapshot(EVENT_ID)).willReturn(Optional.of(snapshot(12L, 4L, 11L).build()));

            EventSnapshotLookupRes result = service.lookupEventSnapshot(EVENT_ID, new EventSnapshotLookupReq(null, null));

            assertThat(result.criteria()).isEqualTo("latest");
            assertThat(result.found()).isTrue();
            assertThat(result.snapshot().versionNo()).isEqualTo(4L);
        }

        @Test
        @DisplayName("asOf 는 KST 로 받아 UTC 로 바꿔 조회하고, 이력이 없으면 found=false")
        void convertsAsOfAndReturnsNotFound() {
            given(snapshotRepository.findEventSnapshotAsOf(EVENT_ID, LocalDateTime.of(2026, 9, 15, 5, 0))).willReturn(Optional.empty());

            EventSnapshotLookupRes result = service.lookupEventSnapshot(EVENT_ID, new EventSnapshotLookupReq(null, LocalDateTime.of(2026, 9, 15, 14, 0)));

            assertThat(result.criteria()).isEqualTo("asOf");
            assertThat(result.found()).isFalse();
            assertThat(result.snapshot()).isNull();
        }

        @Test
        @DisplayName("versionNo 는 (eventId, 버전 번호) 로 조회한다")
        void findsByVersionNo() {
            given(snapshotRepository.findByMiEventIdAndSnapshotVersionNoAndIsDeletedFalse(EVENT_ID, 2L))
                .willReturn(Optional.of(snapshot(7L, 2L, 5L).build()));

            EventSnapshotLookupRes result = service.lookupEventSnapshot(EVENT_ID, new EventSnapshotLookupReq(2L, null));

            assertThat(result.criteria()).isEqualTo("versionNo");
            assertThat(result.snapshot().snapshotId()).isEqualTo(7L);
            then(snapshotRepository).should(never()).findLatestEventSnapshot(any());
        }
    }

    @Nested
    @DisplayName("비교")
    class Compare {

        @Test
        @DisplayName("previous 인데 이전 Snapshot ID 가 없으면 comparable=false 이고 버전 번호로 대신 찾지 않는다")
        void notComparableWithoutPrevious() {
            given(snapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(5L, EVENT_ID))
                .willReturn(Optional.of(snapshot(5L, 1L, null).build()));

            EventSnapshotCompareRes result = service.compareEventSnapshot(EVENT_ID, 5L, EventSnapshotCompareTarget.PREVIOUS);

            assertThat(result.comparable()).isFalse();
            assertThat(result.compared()).isNull();
            assertThat(result.differences()).isEmpty();
            assertThat(result.recordedChangedFields()).containsExactly("event_title");
            then(snapshotRepository).should(never()).findByMiEventIdAndSnapshotVersionNoAndIsDeletedFalse(any(), any());
        }

        @Test
        @DisplayName("값이 다른 필드만 differences 에 담고, BigDecimal 0.5 와 0.5000 은 같다고 본다")
        void reportsOnlyDifferentFields() {
            MiEventSnapshot base = snapshot(12L, 4L, 11L).eventTitle("새 제목").signalConfidence(new BigDecimal("0.5")).build();
            MiEventSnapshot previous = snapshot(11L, 3L, 7L).eventTitle("옛 제목").signalConfidence(new BigDecimal("0.5000")).build();
            given(snapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(12L, EVENT_ID)).willReturn(Optional.of(base));
            given(snapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(11L, EVENT_ID)).willReturn(Optional.of(previous));

            EventSnapshotCompareRes result = service.compareEventSnapshot(EVENT_ID, 12L, EventSnapshotCompareTarget.PREVIOUS);

            assertThat(result.comparable()).isTrue();
            assertThat(result.compared().snapshotId()).isEqualTo(11L);
            assertThat(result.differences()).singleElement().satisfies(diff -> {
                assertThat(diff.field()).isEqualTo("eventName");
                assertThat(diff.column()).isEqualTo("event_title");
                assertThat(diff.baseValue()).isEqualTo("새 제목");
                assertThat(diff.comparedValue()).isEqualTo("옛 제목");
            });
            assertThat(result.sameFields()).contains("signalConfidence", "keySummaries", "detailedAnalysis");
            assertThat(result.recordedChangedFields()).containsExactly("event_title");
        }

        @Test
        @DisplayName("current 는 현재 Event 와 비교하고, 현재 Event 가 없으면 comparable=false")
        void comparesWithCurrentEvent() {
            given(snapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(12L, EVENT_ID))
                .willReturn(Optional.of(snapshot(12L, 4L, 11L).build()));
            given(miEventRepository.findById(EVENT_ID)).willReturn(Optional.empty());

            EventSnapshotCompareRes missing = service.compareEventSnapshot(EVENT_ID, 12L, EventSnapshotCompareTarget.CURRENT);
            assertThat(missing.comparable()).isFalse();

            MiEvent current = mock(MiEvent.class);
            given(current.getEventTitle()).willReturn("현재 제목");
            given(current.getOneLineSummary()).willReturn("요약");
            given(current.getKeyBulletsJson()).willReturn("{\"bullets\": [\"b1\", \"b2\"]}");
            given(current.getDetailAnalysis()).willReturn("상세");
            given(current.getIsSignal()).willReturn(true);
            given(current.getIsNew()).willReturn(false);
            given(current.getIsDeleted()).willReturn(true);
            given(miEventRepository.findById(EVENT_ID)).willReturn(Optional.of(current));

            EventSnapshotCompareRes result = service.compareEventSnapshot(EVENT_ID, 12L, EventSnapshotCompareTarget.CURRENT);

            assertThat(result.comparable()).isTrue();
            assertThat(result.compared().snapshotId()).isNull();
            assertThat(result.differences()).extracting(EventSnapshotCompareRes.FieldDifference::field)
                .contains("eventName", "isEventDeleted", "eventTypeL2Id")
                .doesNotContain("oneLineSummary", "keySummaries", "detailedAnalysis");
        }
    }

    @Test
    @DisplayName("근거 Record 는 Snapshot 소속 확인 후 사본 기준으로 조회하고 isPermitted 를 상품 권한으로 계산한다")
    void getsRecordsWithPermission() {
        given(snapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(12L, EVENT_ID))
            .willReturn(Optional.of(snapshot(12L, 4L, 11L).createDt(LocalDateTime.of(2026, 9, 15, 5, 2, 53)).build()));
        PermissionRes permissions = mock(PermissionRes.class);
        given(authorizationService.getPermissions()).willReturn(permissions);
        given(authorizationService.getCompanyNoWithPriority()).willReturn(1);
        given(permissions.getGoodsViewableMap(1)).willReturn(Map.of(100, true));
        EventSnapshotRecordRow permitted = recordRow(1L, 100, 1);
        EventSnapshotRecordRow deletedSource = recordRow(2L, null, 0);
        given(recordMapRepository.findEventSnapshotRecords(eq(12L), eq("NEWS"), isNull(), isNull(), isNull(), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(permitted, deletedSource), PageRequest.of(0, 10), 2));

        PaginationRes<EventSnapshotRecordRes> result = service.getEventSnapshotRecords(EVENT_ID, 12L,
            new EventSnapshotRecordListReq("NEWS", " ", null, null, null, null));

        assertThat(result.detail()).hasSize(2);
        assertThat(result.detail().get(0).isPermitted()).isTrue();
        assertThat(result.detail().get(0).isSourceAvailable()).isTrue();
        assertThat(result.detail().get(0).snapshotCreatedAt()).isEqualTo(LocalDateTime.of(2026, 9, 15, 14, 2, 53));
        assertThat(result.detail().get(1).isPermitted()).isFalse();
        assertThat(result.detail().get(1).isSourceAvailable()).isFalse();
    }

    @Test
    // [MCP Operator]
    @DisplayName("운영자는 상품 권한 정보를 조회하지 않고 근거 Record 를 모두 isPermitted=false 로 반환한다")
    void getsRecordsWithoutPermissionForOperator() {
        given(snapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(12L, EVENT_ID))
            .willReturn(Optional.of(snapshot(12L, 4L, 11L).build()));
        given(authorizationService.getCurrentRole()).willReturn(Role.OPERATOR);
        EventSnapshotRecordRow row = recordRow(1L, 100, 1);
        given(recordMapRepository.findEventSnapshotRecords(eq(12L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1));

        PaginationRes<EventSnapshotRecordRes> result = service.getEventSnapshotRecords(EVENT_ID, 12L,
            new EventSnapshotRecordListReq(null, null, null, null, null, null));

        assertThat(result.detail()).singleElement().satisfies(record -> assertThat(record.isPermitted()).isFalse());
        then(authorizationService).should(never()).getPermissions();
    }

    @Test
    @DisplayName("기관 전망은 다른 Event 의 snapshotId 면 조회하지 않고 RSC-001")
    void outlooksRequireOwnedSnapshot() {
        given(snapshotRepository.findByMiEventSnapshotIdAndMiEventIdAndIsDeletedFalse(99L, EVENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEventSnapshotOutlooks(EVENT_ID, 99L, null))
            .isInstanceOf(AppErrorException.class);
        then(institutionViewRepository).shouldHaveNoInteractions();
    }

    private static MiEventSnapshot.MiEventSnapshotBuilder snapshot(Long id, Long versionNo, Long previousId) {
        return MiEventSnapshot.builder()
            .miEventSnapshotId(id)
            .miEventId(EVENT_ID)
            .snapshotVersionNo(versionNo)
            .previousMiEventSnapshotId(previousId)
            .createDt(LocalDateTime.of(2026, 9, 15, 5, 0))
            .createUserId("system")
            .updateTriggerCd("RECORD_INGESTION")
            .changedFieldsJson("[\"event_title\"]")
            .eventTypeL2Id(3L)
            .eventTitle("제목")
            .oneLineSummary("요약")
            .keyBulletsJson("{\"bullets\": [\"b1\", \"b2\"]}")
            .detailAnalysis("상세")
            .isSignal(true)
            .isScenarioSuggested(false)
            .isNew(false)
            .isEventDeleted(false)
            .eventCreateUserId("system")
            .eventCreateDt(LocalDateTime.of(2026, 6, 21, 23, 2))
            .eventModifyUserId("system")
            .eventModifyDt(LocalDateTime.of(2026, 9, 15, 5, 0));
    }

    private static EventSnapshotRecordRow recordRow(Long miRecordId, Integer goodsNo, int sourceAvailable) {
        EventSnapshotRecordRow row = mock(EventSnapshotRecordRow.class);
        given(row.getMiRecordId()).willReturn(miRecordId);
        given(row.getGoodsNo()).willReturn(goodsNo);
        given(row.getSourceAvailable()).willReturn(sourceAvailable);
        return row;
    }

}
