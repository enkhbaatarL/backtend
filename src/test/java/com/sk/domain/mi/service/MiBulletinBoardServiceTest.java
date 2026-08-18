package com.sk.domain.mi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.bbs.MiNoticeDetailRes;
import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardUserMappingId;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardNotificationUserRepository;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardRepository;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.service.FileService;

@DisplayName("MiBulletinBoardService 테스트")
@ExtendWith(MockitoExtension.class)
class MiBulletinBoardServiceTest {

    private static final int COMPANY_NO = 10;
    private static final int USER_NO = 20;
    private static final int NOTICE_NO = 30;

    @Mock
    private BulletinBoardRepository bulletinBoardRepository;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private CustomerCompanyRepository customerCompanyRepository;

    @Mock
    private BulletinBoardNotificationUserRepository bulletinBoardNotificationUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileService fileService;

    @Mock
    private BulletinBoard bulletinBoard;

    @Mock
    private User user;

    private MiBulletinBoardService service;

    @BeforeEach
    void setUp() {
        service = new MiBulletinBoardService(
                bulletinBoardRepository,
                uploadedFileRepository,
                customerCompanyRepository,
                bulletinBoardNotificationUserRepository,
                userRepository,
                fileService);
    }

    @Test
    @DisplayName("목록 조회 범위 밖 공지는 상세 조회에서도 거부하고 부수효과를 만들지 않는다")
    void rejectNoticeOutsideListScopeWithoutSideEffects() {
        given(bulletinBoardRepository.findAccessibleNoticeForService(NOTICE_NO, COMPANY_NO))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNotice(COMPANY_NO, USER_NO, NOTICE_NO))
                .isInstanceOfSatisfying(AppErrorException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ResultCode.Error.RSC_NOT_FOUND));

        then(uploadedFileRepository).shouldHaveNoInteractions();
        then(userRepository).shouldHaveNoInteractions();
        then(bulletinBoardNotificationUserRepository).shouldHaveNoInteractions();
        then(fileService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("목록 조회 범위에 포함된 공지는 상세 조회하고 읽음 기록을 남긴다")
    void getNoticeInsideListScope() {
        given(bulletinBoardRepository.findAccessibleNoticeForService(NOTICE_NO, COMPANY_NO))
                .willReturn(Optional.of(bulletinBoard));
        given(bulletinBoard.getBulletinNo()).willReturn(NOTICE_NO);
        given(bulletinBoard.getBulletinCategorySeparateCd()).willReturn("CATEGORY1");
        given(bulletinBoard.getBulletinTitleNm()).willReturn("공지 제목");
        given(bulletinBoard.getBulletinCntt()).willReturn("공지 내용");
        given(uploadedFileRepository.findByFileRelationshipObjectDistinguishNoAndFileRelationshipObjectSeparateCode(
                any(), any(), any())).willReturn(List.of());
        given(userRepository.findByUserNoAndIsDeletedFalse(USER_NO)).willReturn(Optional.of(user));
        given(user.getUserNo()).willReturn(USER_NO);
        given(bulletinBoardNotificationUserRepository.findByIdAndIsDeletedFalse(any(BulletinBoardUserMappingId.class)))
                .willReturn(Optional.empty());

        MiNoticeDetailRes result = service.getNotice(COMPANY_NO, USER_NO, NOTICE_NO);

        assertThat(result.bulletinNo()).isEqualTo(NOTICE_NO);
        assertThat(result.bulletinCategoryName()).isEqualTo("시스템");
        assertThat(result.bulletinTitleNm()).isEqualTo("공지 제목");
        assertThat(result.bulletinCntt()).isEqualTo("공지 내용");
        assertThat(result.fileUrl()).isNull();
        then(bulletinBoardNotificationUserRepository).should()
                .save(any());
        then(fileService).shouldHaveNoInteractions();
    }
}
