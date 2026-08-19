package com.sk.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.service.AdminBulletinBoardService;
import com.sk.domain.admin.validator.NoticeImageUploadValidator;
import com.sk.domain.shared.client.FileCryptoApiClient;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.service.FileEncryptionPolicy;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.ObjectStorageService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.io.InputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("AdminBulletinBoardController 공지 이미지 업로드 테스트")
@ExtendWith(MockitoExtension.class)
class AdminBulletinBoardControllerTest {

    private static final String COMPANY_CD = "SK";
    private static final String END_POINT = "https://storage.example.com";
    private static final String COMMON_BUCKET = "common-bucket";
    private static final String FILE_URL = END_POINT + "/" + COMMON_BUCKET + "/TEMP/image";
    private static final int MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private AdminBulletinBoardService adminBulletinBoardService;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private FileEncryptionPolicy fileEncryptionPolicy;

    @Mock
    private FileCryptoApiClient fileCryptoApiClient;

    private AdminBulletinBoardController controller;

    @BeforeEach
    void setUp() {
        FileService fileService = new FileService(
                uploadedFileRepository,
                objectStorageService,
                fileEncryptionPolicy,
                fileCryptoApiClient);
        ReflectionTestUtils.setField(fileService, "endPoint", END_POINT);
        ReflectionTestUtils.setField(fileService, "commBucketName", COMMON_BUCKET);
        ReflectionTestUtils.setField(fileService, "prefix", "company-");

        controller = new AdminBulletinBoardController(
                authorizationService,
                adminBulletinBoardService,
                fileService,
                new NoticeImageUploadValidator());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validImages")
    @DisplayName("정상 PNG/JPG는 canonical UUID key와 MIME으로 저장하고 기존 응답을 유지한다")
    void uploadsValidImageWithCanonicalStorageValues(
            String description,
            MockMultipartFile file,
            String expectedExtension,
            String expectedContentType) throws Exception {
        given(authorizationService.getCurrentRole()).willReturn(Role.ADMIN);
        given(authorizationService.getCompanyCdWithPriority()).willReturn(COMPANY_CD);
        given(objectStorageService.uploadFile(
                any(InputStream.class),
                eq(COMMON_BUCKET),
                anyString(),
                eq(true),
                eq(expectedContentType),
                eq(file.getSize()))).willReturn(FILE_URL);
        given(uploadedFileRepository.save(any(UploadedFile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        BaseResponse<FileUploadRes> response = controller.uploadFile(file);

        assertThat(response.isStatus()).isTrue();
        assertThat(response.getCode()).isEqualTo(ResultCode.Success.OK.getCode());
        assertThat(response.getData().fileNm()).isEqualTo(file.getOriginalFilename());
        assertThat(response.getData().fileSize()).isEqualTo(file.getSize());
        assertThat(response.getData().fileUrl()).isEqualTo(FILE_URL);

        ArgumentCaptor<String> storageKeyCaptor = ArgumentCaptor.forClass(String.class);
        then(objectStorageService).should().uploadFile(
                any(InputStream.class),
                eq(COMMON_BUCKET),
                storageKeyCaptor.capture(),
                eq(true),
                eq(expectedContentType),
                eq(file.getSize()));
        assertThat(storageKeyCaptor.getValue())
                .matches("TEMP/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\." + expectedExtension);
        then(objectStorageService).should(never()).uploadFile(
                any(MultipartFile.class),
                anyString(),
                anyString(),
                eq(true));

        ArgumentCaptor<UploadedFile> uploadedFileCaptor = ArgumentCaptor.forClass(UploadedFile.class);
        then(uploadedFileRepository).should().save(uploadedFileCaptor.capture());
        UploadedFile savedFile = uploadedFileCaptor.getValue();
        assertThat(savedFile.getStorageFileNm())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\." + expectedExtension);
        assertThat(savedFile.getFileExtNm()).isEqualTo(expectedExtension);
        assertThat(savedFile.getMimeTypeNm()).isEqualTo(expectedContentType);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidImages")
    @DisplayName("잘못된 파일은 Object Storage와 repository 호출 전에 거절한다")
    void rejectsInvalidImageWithoutStorageSideEffects(
            String description,
            MultipartFile file,
            ResultCode.Error expectedError) {
        assertThatThrownBy(() -> controller.uploadFile(file))
                .isInstanceOfSatisfying(AppErrorException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedError));

        then(authorizationService).shouldHaveNoInteractions();
        then(objectStorageService).shouldHaveNoInteractions();
        then(uploadedFileRepository).shouldHaveNoInteractions();
    }

    private static Stream<Arguments> validImages() {
        return Stream.of(
                Arguments.of(
                        "PNG",
                        imageFile("notice.png", "image/png", pngBytes()),
                        "png",
                        "image/png"),
                Arguments.of(
                        "JPG with normalized extension and MIME",
                        imageFile("notice.JPG", " IMAGE/JPEG ", jpegBytes()),
                        "jpg",
                        "image/jpeg"));
    }

    private static Stream<Arguments> invalidImages() {
        return Stream.of(
                Arguments.of("null file", null, ResultCode.Error.REQ_INVALID_PARAMETER),
                Arguments.of("empty file", imageFile("empty.png", "image/png", new byte[0]), ResultCode.Error.FILE_SIZE_ERROR),
                Arguments.of("over 20 MiB", imageFile("large.png", "image/png", new byte[MAX_FILE_SIZE_BYTES + 1]), ResultCode.Error.FILE_SIZE_ERROR),
                Arguments.of("missing extension", imageFile("notice", "image/png", pngBytes()), ResultCode.Error.REQ_INVALID),
                Arguments.of("null original filename", imageFile(null, "image/png", pngBytes()), ResultCode.Error.REQ_INVALID),
                Arguments.of("JSP extension", imageFile("notice.jsp", "image/jpeg", jpegBytes()), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("GIF extension", imageFile("notice.gif", "image/png", pngBytes()), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("SVG extension", imageFile("notice.svg", "image/png", pngBytes()), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("JPEG extension", imageFile("notice.jpeg", "image/jpeg", jpegBytes()), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("JPG with PNG MIME", imageFile("notice.jpg", "image/png", jpegBytes()), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("PNG with JPEG MIME", imageFile("notice.png", "image/jpeg", pngBytes()), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("script disguised as JPG", imageFile("script.jpg", "image/jpeg", "<script>".getBytes()), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("PNG signature with JPG extension", imageFile("notice.jpg", "image/jpeg", pngBytes()), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("JPEG signature with PNG extension", imageFile("notice.png", "image/png", jpegBytes()), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("truncated signature", imageFile("notice.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E}), ResultCode.Error.FILE_FORMAT_ERROR),
                Arguments.of("Content-Type parameter", imageFile("notice.jpg", "image/jpeg; charset=UTF-8", jpegBytes()), ResultCode.Error.FILE_FORMAT_ERROR));
    }

    private static MockMultipartFile imageFile(String originalFilename, String contentType, byte[] content) {
        return new MockMultipartFile("file", originalFilename, contentType, content);
    }

    private static byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x01
        };
    }

    private static byte[] jpegBytes() {
        return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
    }
}
