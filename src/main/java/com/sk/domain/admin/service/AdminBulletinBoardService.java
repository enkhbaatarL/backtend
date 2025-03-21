package com.sk.domain.admin.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.bbs.NoticeCreateReq;
import com.sk.domain.admin.dto.request.bbs.NoticeUpdateReq;
import com.sk.domain.admin.dto.request.bbs.FaqCreateReq;
import com.sk.domain.admin.dto.request.bbs.FaqUpdateReq;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFilterDto;
import com.sk.domain.admin.dto.response.bbs.FaqFilterRes;
import com.sk.domain.admin.dto.response.bbs.NoticeDetailRes;
import com.sk.domain.admin.dto.response.bbs.NoticeFilterRes;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tcm.BulletinBoardCustomerCompanyMapping;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.enums.FaqCategory;
import com.sk.domain.shared.enums.NoticeCategory;
import com.sk.domain.shared.enums.NoticeStatus;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardCustomerCompanyMappingRepository;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardRepository;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;

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
    private final ProviderRepository providerRepository;
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

        List<BulletinBoardFilterDto> target = providerRepository.findByIsDeletedFalseOrderByProviderNmAsc().stream()
                .map(provider -> new BulletinBoardFilterDto(provider.getProviderNo().toString(), provider.getProviderNm()))
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
    public PaginationRes<BulletinBoard> getAllNotices(int page, int size, String category, String status, String target, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<BulletinBoard> noticePage = bulletinBoardRepository.searchBulletinBoards("", "", null, pageable);
        // Page<ProviderRes> providerPage = providerRepository.findByIsDeletedFalseWithGoodsCountOrderByProviderNmAsc(pageable);

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
                .findByBulletinNoAndBulletinTypeCodeAndIsDeletedFalse(noticeNo, "NOTICE")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo)));

        List<Integer> target = bulletinBoardCustomerCompanyMappingRepository
                .findByBulletinBoard(bulletinBoard)
                .stream()
                .map(m -> m.getCustomerCompany().getCustomerCompanyNo())
                .toList();

        UploadedFile uploadedFile = uploadedFileRepository.findByRelationshipObjectInfo(bulletinBoard.getBulletinNo(),
                "BBS").orElse(null);

        return NoticeDetailRes.of(bulletinBoard, target, uploadedFile);
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

        BulletinBoard bulletinBoard = bulletinBoardRepository
                .findByBulletinNoAndBulletinTypeCodeAndIsDeletedFalse(noticeNo, "NOTICE")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo)));

        if (dto.fileNo() == null) {
            adminFileService.deleteFile(bulletinBoard.getBulletinNo(), "BBS");
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
                .findByBulletinNoAndBulletinTypeCodeAndIsDeletedFalse(noticeNo, "NOTICE")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo)));

        bulletinBoardCustomerCompanyMappingRepository.deleteByBulletinNo(bulletinBoard.getBulletinNo());

        bulletinBoard.softDelete();

        return bulletinBoard;
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

        Specification<BulletinBoard> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 카테고리가 "전체"가 아니면 필터 적용
            if (StringUtils.hasText(category) && !"전체".equals(category)) {
                predicates.add(cb.equal(root.get("bulletinCategorySeparateCode"), category));
            }

            // keyword가 있으면 제목 또는 내용으로 검색
            if (StringUtils.hasText(keyword)) {
                String keywordPattern = "%" + keyword + "%";
                Predicate titleLike = cb.like(root.get("bulletinTitleName"), keywordPattern);
                Predicate contentLike = cb.like(root.get("bulletinContent"), keywordPattern);
                predicates.add(cb.or(titleLike, contentLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BulletinBoard> BulletinBoardPage = bulletinBoardRepository.findByBulletinTypeCodeAndIsDeletedFalse(spec, pageable,
                "FAQ");

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
        return bulletinBoardRepository.findByBulletinNoAndBulletinTypeCodeAndIsDeletedFalse(faqNo, "FAQ")
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
                .findByBulletinNoAndBulletinTypeCodeAndIsDeletedFalse(faqNo, "FAQ")
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
                .findByBulletinNoAndBulletinTypeCodeAndIsDeletedFalse(faqNo, "FAQ")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("faq no: %d", faqNo)));

        bulletinBoard.softDelete();

        return bulletinBoard;
    }
}
