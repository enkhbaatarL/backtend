package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.mi.dto.response.MiCompanyLogoRes;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

/**
 * 고객회사 로고를 등록, 교체, 해제한다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class MiCompanyLogoService {

    private static final Set<String> ALLOWED_IMAGE_FORMATS = Set.of("png", "jpeg");

    private final UploadedFileRepository uploadedFileRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final AuthorizationService authorizationService;
    private final FileService fileService;
    private final DataSize maxLogoFileSize;
    private final boolean storageBrowserAccessible;

    public MiCompanyLogoService(UploadedFileRepository uploadedFileRepository,
                                CustomerCompanyRepository customerCompanyRepository,
                                AuthorizationService authorizationService,
                                FileService fileService,
                                @Value("${mi.company-logo.max-file-size:3MB}") DataSize maxLogoFileSize,
                                @Value("${ncp.storage.browser-accessible:false}") boolean storageBrowserAccessible) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.customerCompanyRepository = customerCompanyRepository;
        this.authorizationService = authorizationService;
        this.fileService = fileService;
        this.maxLogoFileSize = maxLogoFileSize;
        this.storageBrowserAccessible = storageBrowserAccessible;
    }

    /**
     * 현재 컨텍스트 회사의 로고를 등록하거나 교체한다.
     * 대상 회사는 토큰 컨텍스트에서만 결정되므로 호출자가 타사를 지정할 수 없다.
     *
     * @param file 로고 파일
     * @return MiCompanyLogoRes
     */
    @Transactional
    public MiCompanyLogoRes changeLogo(MultipartFile file) {
        return changeLogo(authorizationService.getCompanyNoWithPriority(), file);
    }

    /**
     * 지정한 고객회사의 로고를 등록하거나 교체한다.
     * 새 파일을 올린 뒤 교체 이전에 활성 상태였던 로고 파일을 모두 삭제한다.
     * 대상 회사를 인자로 받으므로 호출부에서 타사 접근 권한을 통제해야 한다.
     *
     * @param companyNo 고객회사번호
     * @param file 로고 파일
     * @return MiCompanyLogoRes
     */
    @Transactional
    public MiCompanyLogoRes changeLogo(Integer companyNo, MultipartFile file) {

        // 대상 회사를 먼저 확정한다. 없는 회사면 파일을 읽기 전에 실패해야 원인이 분명하다.
        CustomerCompany customerCompany = getCustomerCompany(companyNo);
        validateLogoFile(file);

        Role role = authorizationService.getCurrentRole();

        // 업로드 이전 시점의 활성 로고만 정리 대상으로 확정한다. 업로드 이후에 조회하면 신규 파일까지 지워진다.
        List<Long> previousLogoFileNos = findActiveLogoFiles(companyNo).stream()
                .map(UploadedFile::getFileNo)
                .toList();

        FileUploadRes uploaded = fileService.uploadCompanyLogo(customerCompany.getCustomerCompanyCd(), role, file, companyNo);
        UploadedFile logoFile = uploadedFileRepository.findByFileNoAndIsDeletedFalse(uploaded.fileNo())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "업로드된 로고 파일을 찾을 수 없습니다. fileNo: %d".formatted(uploaded.fileNo())));

        previousLogoFileNos.forEach(fileService::deleteFile);

        log.info("Company logo changed. companyNo={}, newFileNo={}, removedFileNos={}", companyNo, logoFile.getFileNo(), previousLogoFileNos);

        return MiCompanyLogoRes.of(companyNo, resolveFileUrl(logoFile));
    }

    /**
     * 현재 컨텍스트 회사의 전용 로고를 해제하여 기본 로고를 사용하도록 되돌린다.
     *
     * @return MiCompanyLogoRes
     */
    @Transactional
    public MiCompanyLogoRes removeLogo() {
        return removeLogo(authorizationService.getCompanyNoWithPriority());
    }

    /**
     * 지정한 고객회사의 전용 로고를 해제하여 기본 로고를 사용하도록 되돌린다.
     * 등록된 로고가 없어도 오류 없이 성공하는 멱등 연산이다.
     *
     * @param companyNo 고객회사번호
     * @return MiCompanyLogoRes
     */
    @Transactional
    public MiCompanyLogoRes removeLogo(Integer companyNo) {
        getCustomerCompany(companyNo);

        List<Long> logoFileNos = findActiveLogoFiles(companyNo).stream()
                .map(UploadedFile::getFileNo)
                .toList();

        logoFileNos.forEach(fileService::deleteFile);

        log.info("Company logo removed. companyNo={}, removedFileNos={}", companyNo, logoFileNos);

        return MiCompanyLogoRes.of(companyNo, null);
    }

    /**
     * 현재 컨텍스트 회사의 로고 정보를 조회한다.
     *
     * @return MiCompanyLogoRes
     */
    public MiCompanyLogoRes getLogo() {
        return getLogo(authorizationService.getCompanyNoWithPriority());
    }

    /**
     * 지정한 고객회사의 로고 정보를 조회한다.
     *
     * @param companyNo 고객회사번호
     * @return MiCompanyLogoRes
     */
    public MiCompanyLogoRes getLogo(Integer companyNo) {
        getCustomerCompany(companyNo);
        return MiCompanyLogoRes.of(companyNo, resolveLogoUrl(companyNo));
    }

    /**
     * 고객회사를 조회한다.
     * 존재하지 않는 회사번호로 로고를 다루면 조용히 성공하는 대신 실패시킨다.
     *
     * @param companyNo 고객회사번호
     * @return CustomerCompany
     */
    private CustomerCompany getCustomerCompany(Integer companyNo) {
        if (companyNo == null) throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "고객회사번호가 필요합니다.");
        return customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "customer company no: %d".formatted(companyNo)));
    }

    /**
     * 고객회사의 로고 노출 URL을 만든다.
     * 전용 로고가 없으면 null을 반환하고 화면은 기본 로고를 사용한다.
     *
     * @param companyNo 고객회사번호
     * @return 로고 URL 또는 null
     */
    public String resolveLogoUrl(Integer companyNo) {
        if (companyNo == null) return null;
        List<UploadedFile> logoFiles = findActiveLogoFiles(companyNo);
        if (logoFiles.isEmpty()) return null;
        return resolveFileUrl(logoFiles.get(0));
    }

    /**
     * 실행 환경에 맞는 로고 파일 노출 URL을 만든다.
     * 스토리지 엔드포인트에 브라우저가 직접 닿을 수 있으면 스토리지 URL을 그대로 내려주고,
     * 그렇지 않으면 앞단 웹서버가 중계하는 서비스 도메인의 이미지 경로를 내려준다.
     *
     * @param uploadedFile 로고 파일
     * @return 로고 URL
     */
    private String resolveFileUrl(UploadedFile uploadedFile) {
        return storageBrowserAccessible ? fileService.getUrl(uploadedFile) : fileService.getSecureUrl(uploadedFile);
    }

    /**
     * 고객회사의 활성 로고 파일을 최신순으로 조회한다.
     *
     * @param companyNo 고객회사번호
     * @return 활성 로고 파일 목록, 없으면 빈 목록
     */
    private List<UploadedFile> findActiveLogoFiles(Integer companyNo) {
        return uploadedFileRepository.findActiveRelationshipFilesOrderByFileNoDesc(companyNo, FileRelationshipObjectSeparateCode.CPNYLOGO.getCode());
    }

    private void validateLogoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw AppErrorException.of(ResultCode.Error.FILE_INVALID, "로고 파일이 없습니다.");
        if (file.getSize() > maxLogoFileSize.toBytes()) throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR, "로고 파일은 %dKB 이하만 등록할 수 있습니다.".formatted(maxLogoFileSize.toKilobytes()));
        String originalFileNm = file.getOriginalFilename();
        if (originalFileNm == null || originalFileNm.lastIndexOf('.') < 0) throw AppErrorException.of(ResultCode.Error.FILE_INVALID, "확장자가 없는 파일은 등록할 수 없습니다.");
        String detectedFormat = detectImageFormat(file);
        if (detectedFormat == null || !ALLOWED_IMAGE_FORMATS.contains(detectedFormat)) throw AppErrorException.of(ResultCode.Error.FILE_INVALID, "로고는 PNG, JPEG 형식만 등록할 수 있습니다.");
    }

    /**
     * 파일 내용으로 이미지 포맷을 판별한다.
     * 파일명 확장자나 Content-Type 헤더는 클라이언트가 임의로 보낼 수 있어 신뢰하지 않고,
     * ImageIO가 실제로 디코딩할 수 있는 리더를 찾았는지로 판단한다.
     *
     * @param file 검사할 파일
     * @return 소문자 포맷명, 이미지가 아니면 null
     */
    private String detectImageFormat(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream(); ImageInputStream imageStream = ImageIO.createImageInputStream(inputStream)) {
            if (imageStream == null) return null;

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) return null;

            ImageReader reader = readers.next();

            try {
                return reader.getFormatName().toLowerCase(Locale.ROOT);
            } finally {
                reader.dispose();
            }

        } catch (IOException e) {
            log.error("로고 파일을 읽지 못했습니다.", e);
            throw AppErrorException.of(ResultCode.Error.FILE_INVALID, "로고 파일을 읽지 못했습니다.");
        }
    }

}
