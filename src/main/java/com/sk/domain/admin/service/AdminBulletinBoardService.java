package com.sk.domain.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.bbs.NoticeCreateReq;
import com.sk.domain.admin.dto.request.bbs.NoticeUpdateReq;
import com.sk.domain.admin.dto.request.bbs.FaqCreateReq;
import com.sk.domain.admin.dto.request.bbs.FaqUpdateReq;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardCompanyDto;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFileDto;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFilterDto;
import com.sk.domain.admin.dto.response.bbs.FaqFilterRes;
import com.sk.domain.admin.dto.response.bbs.NoticeDetailRes;
import com.sk.domain.admin.dto.response.bbs.NoticeFilterRes;
import com.sk.domain.admin.dto.response.bbs.NoticeRes;
import com.sk.domain.admin.dto.response.bbs.PopupRes;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tcm.BulletinBoardCustomerCompanyMapping;
import com.sk.domain.shared.enums.FaqCategory;
import com.sk.domain.shared.enums.NoticeCategory;
import com.sk.domain.shared.enums.NoticeStatus;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardCustomerCompanyMappingRepository;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardRepository;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminBulletinBoardService {
    private final BulletinBoardRepository bulletinBoardRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final BulletinBoardCustomerCompanyMappingRepository bulletinBoardCustomerCompanyMappingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;

    private final AdminFileService adminFileService;

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

        List<BulletinBoardFilterDto> target = customerCompanyRepository.findByIsDeletedFalseOrderByCustomerCompanyNmAsc().stream()
                .map(company -> new BulletinBoardFilterDto(company.getCustomerCompanyNo().toString(), company.getCustomerCompanyNm()))
                .collect(Collectors.toList());

        return NoticeFilterRes.of(category, status, target);
    }

    /**
     * 공지사항 목록을 페이지네이션 처리하여 조회한다.
     *
     * @param page 조회할 페이지 번호 (min 1)
     * @param size 조회할 요소 사이즈 (min 1)
     * @param category
     * @param status
     * @param target
     * @param keyword
     * @return PaginationRes<NoticeRes>
     */
    public PaginationRes<NoticeRes> getAllNotices(int page, int size, String category, String status, Integer target, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<NoticeRes> noticePage = bulletinBoardRepository.searchNotice(category, status, target, keyword, pageable);

        return PaginationRes.toRes(noticePage);
    }

    /**
     * 공지사항을 등록한다.
     *
     * @param NoticeCreateReq 공지사항 등록 데이터
     * @return BulletinBoard
     */
    @Transactional
    public BulletinBoard createNotice(NoticeCreateReq dto) {
        if (dto.isWholePost() == false) {
            if (dto.target() == null) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
            }

            if (dto.target().size() == 0) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
            }
        }

        // 팝업 1개만 유지
        if (dto.isPopupUse()) {
            bulletinBoardRepository.findByBulletinTypeCdAndIsDeletedFalse("NOTICE").stream().forEach(b -> b.updateIsPopupUse(false));
        }

        BulletinBoard savedBulletinBoard = bulletinBoardRepository.save(BulletinBoard.of(dto));

        if (dto.fileNo() != null) {
            adminFileService.updateFile(dto.fileNo(), savedBulletinBoard.getBulletinNo(), "BBS");
        }

        if (dto.isWholePost()) {
            //
        } else {
            if (dto.target() != null) {
                dto.target().stream()
                        .forEach(companyNo -> {
                            customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                                    .ifPresent(company -> {
                                        bulletinBoardCustomerCompanyMappingRepository
                                                .save(BulletinBoardCustomerCompanyMapping.of(savedBulletinBoard, company));
                                    });
                        });
            }
        }

        return savedBulletinBoard;
    }

    /**
    * 공지사항을 상세 조회한다.
    *
    * @param noticeNo 공지사항 번호
    * @return NoticeDetailRes
    */
    public NoticeDetailRes getNotice(int noticeNo) {
        BulletinBoard bulletinBoard = bulletinBoardRepository
                .findByBulletinNoAndBulletinTypeCdAndIsDeletedFalse(noticeNo, "NOTICE")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo)));

        List<BulletinBoardCompanyDto> target = bulletinBoardCustomerCompanyMappingRepository.findCustomCompanyByBulletinBoard(bulletinBoard);

        List<BulletinBoardFileDto> fileInfo = uploadedFileRepository.findByRelationshipObjectInfo(bulletinBoard.getBulletinNo(), "BBS")
                .stream()
                .map(f -> new BulletinBoardFileDto(
                        f.getFileNo(),
                        f.getOriginalFileNm(),
                        f.getFileSize(),
                        adminFileService.getPresignedUrl(f.getFileNo())))
                .toList();

        return NoticeDetailRes.of(bulletinBoard, target, fileInfo);
    }

    /**
    * 공지사항을 수정한다.
    *
    * @param NoticeUpdateReq 공지사항 수정 데이터
    * @param noticeNo 공지사항 번호
    * @return BulletinBoard
    */
    @Transactional
    public BulletinBoard updateNotice(NoticeUpdateReq dto, int noticeNo) {
        if (dto.isWholePost() != null) {
            if (dto.isWholePost() == false) {
                if (dto.target() == null) {
                    throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
                }

                if (dto.target().size() == 0) {
                    throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
                }
            }
        }

        BulletinBoard bulletinBoard = bulletinBoardRepository
                .findByBulletinNoAndBulletinTypeCdAndIsDeletedFalse(noticeNo, "NOTICE")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo)));

        if (dto.fileNo() == null) {
            adminFileService.deleteFiles(bulletinBoard.getBulletinNo(), "BBS");
        } else {
            adminFileService.updateFile(dto.fileNo(), bulletinBoard.getBulletinNo(), "BBS");
        }

        if (dto.isWholePost() != null) {
            if (bulletinBoard.getIsWholePost() != dto.isWholePost()) {
                if (dto.isWholePost()) {
                    bulletinBoardCustomerCompanyMappingRepository.deleteByBulletinNo(bulletinBoard.getBulletinNo());
                } else {
                    if (dto.target() != null) {
                        dto.target().stream()
                                .forEach(companyNo -> {
                                    customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                                            .ifPresent(company -> {
                                                bulletinBoardCustomerCompanyMappingRepository
                                                        .save(BulletinBoardCustomerCompanyMapping.of(bulletinBoard, company));
                                            });
                                });
                    } else {
                        AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, String.format("notice no: %d", noticeNo));
                    }
                }
            }
        }

        // 게시중 -> 게시중단 -> 재게시 시 재게시일을 등록일로 수정
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate today = LocalDate.now();
        LocalDate startDate = LocalDate.parse(bulletinBoard.getBulletinValidBeginDt(), formatter);
        LocalDate endDate = LocalDate.parse(bulletinBoard.getBulletinValidEndDt(), formatter);
        LocalDate newStartDate = LocalDate.parse(dto.bulletinValidBeginDt(), formatter);
        LocalDate newEndDate = LocalDate.parse(dto.bulletinValidEndDt(), formatter);

        if (today.isBefore(startDate) || today.isAfter(endDate)) { // 게시중단 인 경우
            if (today.isAfter(newStartDate) || today.isBefore(newEndDate)) { // 재게시
                bulletinBoard.updateCreateDt(LocalDateTime.now()); // 등록일 수정
            }
        }

        // 팝업 1개만 유지
        if (bulletinBoard.getIsPopupUse() != dto.isPopupUse()) {
            if (dto.isPopupUse()) {
                bulletinBoardRepository.findByBulletinTypeCdAndIsDeletedFalse("NOTICE").stream().forEach(b -> b.updateIsPopupUse(false));
            }
        }

        bulletinBoard.update(dto);

        return bulletinBoard;
    }

    /**
    * 공지사항을 삭제한다.
    *
    * @param noticeNo 공지사항 번호
    * @return BulletinBoard
    */
    @Transactional
    public BulletinBoard deleteNotice(int noticeNo) {
        BulletinBoard bulletinBoard = bulletinBoardRepository
                .findByBulletinNoAndBulletinTypeCdAndIsDeletedFalse(noticeNo, "NOTICE")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo)));

        bulletinBoardCustomerCompanyMappingRepository.deleteByBulletinNo(bulletinBoard.getBulletinNo());

        adminFileService.deleteFiles(bulletinBoard.getBulletinNo(), "BBS");

        bulletinBoard.softDelete();

        return bulletinBoard;
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

        List<BulletinBoardFileDto> fileInfo = uploadedFileRepository.findByRelationshipObjectInfo(bulletinBoard.getBulletinNo(), "BBS")
                .stream()
                .map(f -> new BulletinBoardFileDto(
                        f.getFileNo(),
                        f.getOriginalFileNm(),
                        f.getFileSize(),
                        adminFileService.getPresignedUrl(f.getFileNo())))
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
     * @return PaginationRes<BulletinBoard>
     */
    public PaginationRes<BulletinBoard> getAllFaq(int page, int size, String category, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<BulletinBoard> BulletinBoardPage = bulletinBoardRepository.searchFaq(category, keyword, pageable);

        return PaginationRes.toRes(BulletinBoardPage);
    }

    /**
     * FAQ를 등록한다.
     *
     * @param FaqCreateReq FAQ 등록 데이터
     * @return BulletinBoard
     */
    @Transactional
    public BulletinBoard createFaq(FaqCreateReq dto) {
        return bulletinBoardRepository.save(BulletinBoard.of(dto));
    }

    /**
    * FAQ를 상세 조회한다.
    *
    * @param faqNo FAQ 번호
    * @return BulletinBoard
    */
    public BulletinBoard getFaq(int faqNo) {
        return bulletinBoardRepository.findByBulletinNoAndBulletinTypeCdAndIsDeletedFalse(faqNo, "FAQ")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("faq no: %d", faqNo)));
    }

    /**
    * FAQ를 수정한다.
    *
    * @param FaqUpdateReq FAQ 수정 데이터
    * @param FAQ 번호
    * @return BulletinBoard
    */
    @Transactional
    public BulletinBoard updateFaq(FaqUpdateReq dto, int faqNo) {

        BulletinBoard bulletinBoard = bulletinBoardRepository
                .findByBulletinNoAndBulletinTypeCdAndIsDeletedFalse(faqNo, "FAQ")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("faq no: %d", faqNo)));

        bulletinBoard.update(dto);

        return bulletinBoard;
    }

    /**
    * FAQ를 삭제한다.
    *
    * @param faqNo FAQ 번호
    * @return BulletinBoard
    */
    @Transactional
    public BulletinBoard deleteFaq(int faqNo) {
        BulletinBoard bulletinBoard = bulletinBoardRepository
                .findByBulletinNoAndBulletinTypeCdAndIsDeletedFalse(faqNo, "FAQ")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("faq no: %d", faqNo)));

        bulletinBoard.softDelete();

        return bulletinBoard;
    }
}
