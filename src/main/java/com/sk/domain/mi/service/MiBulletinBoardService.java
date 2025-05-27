package com.sk.domain.mi.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFileDto;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFilterDto;
import com.sk.domain.admin.dto.response.bbs.FaqFilterRes;
import com.sk.domain.admin.dto.response.bbs.NoticeFilterRes;
import com.sk.domain.admin.dto.response.bbs.PopupRes;
import com.sk.domain.mi.dto.response.bbs.MiFaqRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeDetailRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeRes;
import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardCustomerCompanyMappingId;
import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardUserMappingId;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tcm.BulletinBoardNotificationUser;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.FaqCategory;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.NoticeCategory;
import com.sk.domain.shared.enums.NoticeStatus;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardCustomerCompanyMappingRepository;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardNotificationUserRepository;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardRepository;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiBulletinBoardService {
    private final BulletinBoardRepository bulletinBoardRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final BulletinBoardCustomerCompanyMappingRepository bulletinBoardCustomerCompanyMappingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final BulletinBoardNotificationUserRepository bulletinBoardNotificationUserRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    /**
     * 공지사항에서 사용 할 필터를 조회한다.
     *
     * @return NoticeFilterRes
     */
    public NoticeFilterRes getNoticeFilter() {
        List<BulletinBoardFilterDto> category = Arrays.stream(NoticeCategory.values())
                .map(cat -> new BulletinBoardFilterDto(cat.getCode(), cat.getName()))
                .collect(Collectors.toList());

        List<BulletinBoardFilterDto> status = Arrays.stream(NoticeStatus.values())
                .map(cat -> new BulletinBoardFilterDto(cat.getCode(), cat.getName()))
                .collect(Collectors.toList());

        List<BulletinBoardFilterDto> target = customerCompanyRepository
                .findByIsDeletedFalseAndIsMipManagementCompanyFalseOrderByCustomerCompanyNmAsc().stream()
                .map(company -> new BulletinBoardFilterDto(company.getCustomerCompanyNo().toString(), company.getCustomerCompanyNm()))
                .collect(Collectors.toList());

        return NoticeFilterRes.of(category, status, target);
    }

    /**
     * 공지사항 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param user 사용자 정보
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @param customerCompanyNo 고객사 번호
     * @param category
     * @param keyword
     * @return PaginationRes<MiNoticeRes>
     */
    public PaginationRes<MiNoticeRes> getAllNotices(int companyNo, int userNo, Role role, int page, int size, String category, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);

        return PaginationRes.toRes(bulletinBoardRepository.findAllNoticeForServicePage(companyNo, userNo, category, keyword, pageable));
    }

    /**
    * 공지사항을 상세 조회한다.
    *
    * @param user 사용자 정보
    * @param customerCompanyNo 고객사 번호
    * @param noticeNo 공지사항 번호
    * @return MiNoticeDetailRes
    */
    @Transactional
    public MiNoticeDetailRes getNotice(int companyNo, int userNo, int noticeNo) {

        BulletinBoard bulletinBoard = bulletinBoardRepository
                .findByBulletinNoAndBulletinTypeCdAndIsDeletedFalse(noticeNo, "NOTICE")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo)));

        String categoryCode = bulletinBoard.getBulletinCategorySeparateCd();
        String categoryName = NoticeCategory.getNameByCode(categoryCode);
        if (categoryName == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo));
        }

        if (bulletinBoard.getIsWholePost() == false) {
            BulletinBoardCustomerCompanyMappingId bulletinBoardCustomerCompanyMappingId = new BulletinBoardCustomerCompanyMappingId(noticeNo,
                    companyNo);

            bulletinBoardCustomerCompanyMappingRepository.findById(bulletinBoardCustomerCompanyMappingId)
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo)));
        }

        UploadedFile uploadFile = uploadedFileRepository
                .findByFileRelationshipObjectDistinguishNoAndFileRelationshipObjectSeparateCode(
                        bulletinBoard.getBulletinNo(),
                        FileRelationshipObjectSeparateCode.BBS.getCode());

        User user = userRepository.findByUserNoAndIsDeletedFalse(userNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "사용자 정보를 찾을 수 없습니다."));

        BulletinBoardUserMappingId bulletinBoardUserMappingId = new BulletinBoardUserMappingId(bulletinBoard.getBulletinNo(), user.getUserNo());

        if (!bulletinBoardNotificationUserRepository.findByIdAndIsDeletedFalse(bulletinBoardUserMappingId).isPresent()) {
            BulletinBoardNotificationUser bulletinBoardNotificationUser = BulletinBoardNotificationUser.builder()
                    .id(new BulletinBoardUserMappingId(bulletinBoard.getBulletinNo(), user.getUserNo()))
                    .bulletinBoard(bulletinBoard)
                    .user(user)
                    .isRead(true)
                    .build();

            bulletinBoardNotificationUserRepository.save(bulletinBoardNotificationUser);
        }

        return MiNoticeDetailRes.of(bulletinBoard.getBulletinNo(), categoryName, bulletinBoard.getBulletinTitleNm(),
                bulletinBoard.getBulletinCntt(), uploadFile != null ? fileService.getUrl(uploadFile) : null);
    }

    /**
    * 팝업용 공지사항을 조회한다.
    *
    * @param
    * @return PopupRes
    */
    public PopupRes getPopup() {
        BulletinBoard bulletinBoard = bulletinBoardRepository.findByIsPopupUseTrue().orElse(null);
        if (bulletinBoard == null) {
            return null;
        }

        List<BulletinBoardFileDto> fileInfo = uploadedFileRepository
                .findByRelationshipObjectInfo(bulletinBoard.getBulletinNo(), FileRelationshipObjectSeparateCode.BBS.getCode())
                .stream()
                .map(f -> new BulletinBoardFileDto(
                        f.getFileNo(),
                        f.getOriginalFileNm(),
                        f.getFileSize(),
                        fileService.getUrl(f.getFileNo())))
                .toList();

        return PopupRes.of(bulletinBoard, fileInfo);
    }

    /**
     * FAQ에서 사용 할 필터를 조회한다.
     *
     * @return FaqFilterRes
     */
    public FaqFilterRes getFaqFilter() {
        List<BulletinBoardFilterDto> category = Arrays.stream(FaqCategory.values())
                .map(cat -> new BulletinBoardFilterDto(cat.getCode(), cat.getName()))
                .collect(Collectors.toList());

        return FaqFilterRes.of(category);
    }

    /**
     * FAQ 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @param category 조회할 카테고리
     * @param keyword 조회할 단어
     * @return PaginationRes<MiFaqRes>
     */
    public PaginationRes<MiFaqRes> getAllFaq(int page, int size, String category, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);

        return PaginationRes.toRes(bulletinBoardRepository.findAllFaqForServicePage(category, keyword, pageable));
    }
}
