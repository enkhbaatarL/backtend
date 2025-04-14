package com.sk.domain.mi.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardCompanyDto;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFileDto;
import com.sk.domain.admin.dto.response.bbs.FaqRes;
import com.sk.domain.admin.service.AdminFileService;
import com.sk.domain.mi.dto.response.bbs.MiFaqRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeDetailRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeRes;
import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardCustomerCompanyMappingId;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
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
    private final AdminFileService adminFileService;

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
    public PaginationRes<MiNoticeRes> getAllNotices(User user, int page, int size, int customerCompanyNo, String category, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);

        int companyNo = 0;
        if (RoleUtil.isAdmin(user)) {
            companyNo = customerCompanyNo;
        } else if (RoleUtil.isUser(user)) {
            companyNo = user.getCustomerCompany().getCustomerCompanyNo();
        } else {
            throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
        }

        return PaginationRes.toRes(bulletinBoardRepository.findAllNoticeForService(companyNo, category, keyword, pageable));
    }

    /**
    * 공지사항을 상세 조회한다.
    *
    * @param user 사용자 정보
    * @param customerCompanyNo 고객사 번호
    * @param noticeNo 공지사항 번호
    * @return MiNoticeDetailRes
    */
    public MiNoticeDetailRes getNotice(User user, int customerCompanyNo, int noticeNo) {
        int companyNo = 0;
        if (RoleUtil.isAdmin(user)) {
            companyNo = customerCompanyNo;
        } else if (RoleUtil.isUser(user)) {
            companyNo = user.getCustomerCompany().getCustomerCompanyNo();
        } else {
            throw AppErrorException.of(ResultCode.Error.AUTH_ACCESS_DENIED);
        }

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
                        FileRelationshipObjectSeparateCode.BBS.getCode())
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("notice no: %d", bulletinBoard.getBulletinNo())));

        return MiNoticeDetailRes.of(bulletinBoard.getBulletinNo(), bulletinBoard.getBulletinTitleNm(),
                bulletinBoard.getBulletinCntt(), uploadFile != null ? adminFileService.getUrl(uploadFile.getFileNo()) : null);
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

        return PaginationRes.toRes(bulletinBoardRepository.findAllFaqForService(category, keyword, pageable));
    }
}
