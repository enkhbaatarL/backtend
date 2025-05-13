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
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFilterDto;
import com.sk.domain.admin.dto.response.bbs.FaqFilterRes;
import com.sk.domain.admin.dto.response.bbs.NoticeFilterRes;
import com.sk.domain.mi.dto.response.bbs.MiFaqRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeDetailRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeRes;
import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardCustomerCompanyMappingId;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.FaqCategory;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.NoticeCategory;
import com.sk.domain.shared.enums.NoticeStatus;
import com.sk.domain.shared.enums.Role;
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
public class MiBulletinBoardService {
    private final BulletinBoardRepository bulletinBoardRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final BulletinBoardCustomerCompanyMappingRepository bulletinBoardCustomerCompanyMappingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final MiFileService miFileService;

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
    public PaginationRes<MiNoticeRes> getAllNotices(int companyNo, Role role, int page, int size, String category, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);

        return PaginationRes.toRes(bulletinBoardRepository.findAllNoticeForServicePage(companyNo, category, keyword, pageable));
    }

    /**
    * 공지사항을 상세 조회한다.
    *
    * @param user 사용자 정보
    * @param customerCompanyNo 고객사 번호
    * @param noticeNo 공지사항 번호
    * @return MiNoticeDetailRes
    */
    public MiNoticeDetailRes getNotice(int companyNo, int noticeNo) {

        BulletinBoard bulletinBoard = bulletinBoardRepository
                .findByBulletinNoAndBulletinTypeCdAndIsDeletedFalse(noticeNo, "NOTICE")
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", noticeNo)));

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

        return MiNoticeDetailRes.of(bulletinBoard.getBulletinNo(), bulletinBoard.getBulletinTitleNm(),
                bulletinBoard.getBulletinCntt(), uploadFile != null ? miFileService.getUrl(uploadFile) : null);
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
