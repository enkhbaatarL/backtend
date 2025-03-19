package com.sk.domain.admin.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.bbs.BulletinBoardCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFilterDto;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFilterRes;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardRes;
import com.sk.domain.admin.dto.response.provider.ProviderCreateRes;
import com.sk.domain.admin.dto.response.provider.ProviderDeleteRes;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.admin.dto.response.provider.ProviderUpdateRes;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.enums.FAQCategory;
import com.sk.domain.shared.enums.NoticeCategory;
import com.sk.domain.shared.enums.NoticeStatus;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardCustomerCompanyMappingRepository;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardRepository;
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
  private final BulletinBoardCustomerCompanyMappingRepository bulletinBoardCustomerCompanyMappingRepository;
  private final ProviderRepository providerRepository;
  private final CustomerCompanyRepository customerCompanyRepository;

  private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");
  private static final long MAX_SIZE = 20 * 1024 * 1024;

  /**
   * 공지사항에서 사용 할 필터를 조회한다.
   *
   * @return BulletinBoardFilterRes
   */
  public BulletinBoardFilterRes getNoticeFilter() {
    List<BulletinBoardFilterDto> category = Arrays.stream(NoticeCategory.values())
        .map(cat -> new BulletinBoardFilterDto(cat.getCode(), cat.getName()))
        .collect(Collectors.toList());

    List<BulletinBoardFilterDto> status = Arrays.stream(NoticeStatus.values())
        .map(cat -> new BulletinBoardFilterDto(cat.getCode(), cat.getName()))
        .collect(Collectors.toList());

    List<BulletinBoardFilterDto> target = providerRepository.findByIsDeletedFalseOrderByProviderNmAsc().stream()
        .map(provider -> new BulletinBoardFilterDto(provider.getProviderNo().toString(), provider.getProviderNm()))
        .collect(Collectors.toList());

    return BulletinBoardFilterRes.of(category, status, target);
  }

  /**
   * 공지사항 목록을 페이지네이션 처리하여 조회한다.
   *
   * @param page 조회할 페이지 번호 (min 1)
   * @param size 조회할 요소 사이즈 (min 1)
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
   * @param BulletinBoardCreateReq 공지사항 등록 데이터
   * @return BulletinBoard
   */
  @Transactional
  public BulletinBoard createNotice(BulletinBoardCreateReq dto, MultipartFile file) {
    if (file != null) {
      String fileName = file.getOriginalFilename();
      if (fileName == null || fileName.lastIndexOf('.') == -1) {
        throw AppErrorException.of(ResultCode.Error.REQ_INVALID);
      }

      String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
      if (!ALLOWED_EXTENSIONS.contains(extension)) {
        throw AppErrorException.of(ResultCode.Error.REQ_NOT_ALLOWED_FILE_FORMAT);
      }

      if (file.getSize() > MAX_SIZE) {
        throw AppErrorException.of(ResultCode.Error.REQ_NOT_ALLOWED_FILE_SIZE);
      }
    }

    // Provider provider = providerRepository.save(Provider.of(providerReq));

    // bulletinBoardRepository

    // return ProviderCreateRes.of(provider);
    return BulletinBoard.builder().build();
  }

  /**
  * 공지사항을 상세 조회한다.
  *
  * @param providerNo 데이타 프로바이더 번호
  * @return ProviderDetailRes
  */
  public ProviderDetailRes geNotice(int providerNo) {
    return providerRepository.findByProviderNoAndIsDeletedFalseWithUser(providerNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));
  }

  /**
  * 공지사항을 수정한다.
  *
  * @param ProviderUpdateReq 데이타 프로바이더 등록 데이터
  * @param providerNo 데이타 프로바이더 번호
  * @return ProviderUpdateRes
  */
  @Transactional
  public ProviderUpdateRes updateProvider(ProviderUpdateReq providerReq, int providerNo) {

    Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

    provider.update(providerReq);

    return ProviderUpdateRes.of(provider);
  }

  /**
  * 공지사항을 삭제한다.
  *
  * @param bulletinNo 게시글 번호
  * @return ProviderDeleteRes
  */
  @Transactional
  public BulletinBoard deleteProvider(int bulletinNo) {
    BulletinBoard bulletinBoard = bulletinBoardRepository.findByBulletinNo(bulletinNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("bulletin no: %d", bulletinNo)));

            if bulletinBoard

    provider.softDelete();

    return ProviderDeleteRes.of(provider);
  }

  /**
   * FAQ에서 사용 할 필터를 조회한다.
   *
   * @return BulletinBoardFilterRes
   */
  public BulletinBoardFilterRes getFaqFilter() {
    List<BulletinBoardFilterDto> category = Arrays.stream(FAQCategory.values())
        .map(cat -> new BulletinBoardFilterDto(cat.getCode(), cat.getName()))
        .collect(Collectors.toList());

    return BulletinBoardFilterRes.of(category, null, null);
  }

  /**
   * FAQ 목록을 페이지네이션 처리하여 조회한다.
   *
   * @param page 조회할 페이지 번호 (min 1)
   * @param size 조회할 요소 사이즈 (min 1)
   * @return PaginationRes<NoticeRes>
   */
  public PaginationRes<BulletinBoard> getAllFaq(int page, int size, String category) {
    Pageable pageable = PageRequest.of(page - 1, size);

    Page<BulletinBoard> noticePage = bulletinBoardRepository.searchBulletinBoards("", "", null, pageable);
    // Page<ProviderRes> providerPage = providerRepository.findByIsDeletedFalseWithGoodsCountOrderByProviderNmAsc(pageable);

    return PaginationRes.toRes(noticePage);
  }

}
