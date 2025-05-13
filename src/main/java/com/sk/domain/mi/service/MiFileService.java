package com.sk.domain.mi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.net.MalformedURLException;
import java.net.URL;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiFileService {
    private final UploadedFileRepository uploadedFileRepository;
    private final MiObjectStorageService miObjectStorageService;

    /**
    * 파일을 다운로드 한다.
    *
    * @param fileNo 파일번호
    * @return FileDownloadRes
    */
    public FileDownloadRes downloadFile(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String fileName = getFileName(uploadedFile.getStoragePathNm());

            byte[] fileData = miObjectStorageService.downloadFile(bucketName, fileName);

            return FileDownloadRes.of(uploadedFile.getFileNo(), uploadedFile.getOriginalFileNm(), uploadedFile.getFileSize(), fileData);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }
    }

    /**
    * 파일에 대한 URL을 생성한다.
    *
    * @param fileNo 파일번호
    * @return URL
    */
    public String getUrl(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));
        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String fileName = getFileName(uploadedFile.getStoragePathNm());

            return miObjectStorageService.getUrl(bucketName, fileName);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }
    }

    public String getUrl(UploadedFile uploadedFile) {
        if (uploadedFile == null) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "업로드된 파일이 없습니다.");
        }

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String fileName = getFileName(uploadedFile.getStoragePathNm());

            return miObjectStorageService.getUrl(bucketName, fileName);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }
    }

    /**
     * URL에서 경로(path) 부분을 "/" 기준으로 분리한 배열을 반환합니다.
     * 예시: "/버킷명/폴더/파일명.jpg" → ["", "버킷명", "폴더", "파일명.jpg"]
     *
     * @param path URL 문자열
     * @return 경로를 분리한 문자열 배열
     * @throws MalformedURLException 잘못된 URL인 경우
     */
    private String[] extractPathSegments(String path) throws MalformedURLException {
        URL url = new URL(path);
        return url.getPath().split("/");
    }

    /**
     * URL에서 버킷 이름(경로상의 두 번째 요소)을 추출합니다.
     * 예: "https://kr.object.ncloudstorage.com/버킷명/폴더/파일명.jpg" → "버킷명"
     *
     * @param path URL 문자열
     * @return 버킷 이름
     * @throws MalformedURLException URL 파싱 실패 시
     * @throws IllegalArgumentException URL에 필요한 정보가 없는 경우
     */
    private String getBucketName(String path) throws MalformedURLException {
        String[] segments = extractPathSegments(path);
        if (segments.length < 2) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "URL에 버킷 이름이 포함되어 있지 않습니다.");
        }
        return segments[1];
    }

    /**
     * URL에서 파일 경로와 파일 이름(버킷 이후의 나머지 경로)을 추출합니다.
     * 예: "https://kr.object.ncloudstorage.com/버킷명/폴더/파일명.jpg" → "폴더/파일명.jpg"
     *
     * @param path URL 문자열
     * @return 파일 경로 및 파일 이름
     * @throws MalformedURLException URL 파싱 실패 시
     * @throws IllegalArgumentException URL에 파일 경로 정보가 없는 경우
     */
    private String getFileName(String path) throws MalformedURLException {
        String[] segments = extractPathSegments(path);
        if (segments.length < 3) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "URL에 파일 경로 정보가 포함되어 있지 않습니다.");
        }
        return String.join("/", Arrays.copyOfRange(segments, 2, segments.length));
    }
}
