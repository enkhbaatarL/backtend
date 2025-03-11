package com.sk.domain.admin.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.CompanyInfoReq;
import com.sk.domain.admin.dto.request.ProviderReq;
import com.sk.domain.admin.dto.response.CompanyInfoRes;
import com.sk.domain.admin.dto.response.ProviderRes;
import com.sk.domain.shared.entity.master.tif.CompanyInfo;
import com.sk.domain.shared.repository.master.tif.CompanyInfoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProviderService {
  // private final ProviderRepository repository;

  /**
   * 데이타 프로바이더 목록을 페이지네이션 처리하여 조회한다.
   *
   * @param page 조회할 페이지 번호 (min 1)
   * @param size 조회할 요소 사이즈 (min 1)
   * @return PaginationRes<ProviderRes>
   */
  public PaginationRes<ProviderRes> all(int page, int size) {

    // Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE,
    // Sort.by(Sort.Direction.DESC, criteria));
    // Page<ReviewResponseDto> page =
    // reviewRepository.findAll(pageable).map(ReviewResponseDto::from);

    // return
    // PaginationRes.toRes(companyInfoRepository.findCompaniesInfoByConditions(page,
    // size));
  }

  /**
   * 데이타 프로바이더를 등록한다.
   *
   * @param ProviderReq 데이타 프로바이더 등록 데이터
   * @return ProviderRes 등록 된 데이타 프로바이더 데이터
   */
  @Transactional
  public ProviderRes newProvider(ProviderReq providerReq) {

    // companyInfoRepository.findByCompanyCd(companyInfoReq.companyCd())
    // .ifPresent(company -> {
    // throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "id already
    // exists.");
    // });

    // CompanyInfo savedEntity =
    // companyInfoRepository.save(CompanyInfo.of(companyInfoReq));

    // return CompanyInfoRes.of(savedEntity);
  }

  /**
   * 데이타 프로바이더를 상세 조회한다.
   *
   * @param id 데이타 프로바이더의 id
   * @return ProviderRes
   */
  public ProviderRes one(Long id) {
    //
  }

  /**
   * 데이타 프로바이더를 수정한다.
   *
   * @param ProviderReq 데이타 프로바이더 등록 데이터
   * @param id          데이타 프로바이더의 id
   * @return ProviderRes
   */
  public ProviderRes replaceProvider(ProviderReq providerReq, Long id) {

    // return repository.findById(id)
    // .map(employee -> {
    // employee.setName(newEmployee.getName());
    // employee.setRole(newEmployee.getRole());
    // return repository.save(employee);
    // })
    // .orElseGet(() -> {
    // return repository.save(newEmployee);
    // });
  }

  /**
   * 데이타 프로바이더를 삭제한다.
   *
   * @param id 데이타 프로바이더의 id
   * @return
   */
  public void deleteProvider(Long id) {
    //
  }
}
