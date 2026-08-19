package com.sk.domain.admin.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("NoticeImageUploadValidator 테스트")
class NoticeImageUploadValidatorTest {

    private final NoticeImageUploadValidator validator = new NoticeImageUploadValidator();

    @Test
    @DisplayName("signature 검사는 최대 8바이트만 읽고 stream을 닫는다")
    void readsAtMostEightSignatureBytesAndClosesStream() throws IOException {
        byte[] image = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x01, 0x02, 0x03, 0x04
        };
        TrackingInputStream inputStream = new TrackingInputStream(image);
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn((long) image.length);
        given(file.getOriginalFilename()).willReturn("image.PNG");
        given(file.getContentType()).willReturn(" IMAGE/PNG ");
        given(file.getInputStream()).willReturn(inputStream);

        ValidatedNoticeImageUpload result = validator.validate(file);

        assertThat(result.extension()).isEqualTo("png");
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(inputStream.getReadCount()).isEqualTo(8);
        assertThat(inputStream.isClosed()).isTrue();
    }

    @Test
    @DisplayName("signature stream을 읽을 수 없으면 FILE_INVALID로 거절한다")
    void rejectsUnreadableSignature() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(8L);
        given(file.getOriginalFilename()).willReturn("image.png");
        given(file.getContentType()).willReturn("image/png");
        given(file.getInputStream()).willThrow(new IOException("unreadable"));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(AppErrorException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ResultCode.Error.FILE_INVALID));
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {

        private int readCount;
        private boolean closed;

        private TrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public synchronized int read(byte[] bytes, int offset, int length) {
            int count = super.read(bytes, offset, length);
            if (count > 0) {
                readCount += count;
            }
            return count;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        private int getReadCount() {
            return readCount;
        }

        private boolean isClosed() {
            return closed;
        }
    }
}
