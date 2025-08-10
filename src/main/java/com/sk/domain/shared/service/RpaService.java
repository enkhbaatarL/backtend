package com.sk.domain.shared.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.dto.request.rpa.ComponentInfoReq;
import com.sk.domain.shared.dto.request.rpa.ProviderDataColumnReq;
import com.sk.domain.shared.dto.response.rpa.ComponentInfoRes;
import com.sk.domain.shared.dto.response.rpa.ProviderDataColumnRes;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;
import com.sk.domain.shared.repository.master.tcm.ComponentInfoRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderDataColumnRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RpaService {
    private final ComponentInfoRepository componentInfoRepository;
    private final ProviderDataColumnRepository providerDataColumnRepository;
    private final ProviderRepository providerRepository;

    /**
     * 정형 데이터 컴포넌트 생성
     * 컴포넌트에서 테이블 Select : 순서에 따라 그래프로 그리기 위한 컬럼 : x축, y축_1, y축_2, y축_3, y축_4
     * tableColumnListNm : 프로바이더 데이터 컬럼 내역 테이블에 미리 칼럼들이 정의되어야 함
     * 
     * @param componentInfo
     * @return
     */
    @Transactional
    public ComponentInfoRes createComponentInfo(ComponentInfoReq.Insert componentInfoReq) {

        List<String> tableColumnList = Arrays.stream(componentInfoReq.tableColumnListNm().split(","))
                .map(String::trim)
                .filter(column -> !column.isEmpty())
                .collect(Collectors.toList());

        if (tableColumnList.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "테이블 컬럼 목록이 없습니다.");
        }

        // 3. DB에서 dataColumnExpn이 포함된 엔티티 조회
        List<ProviderDataColumn> foundColumns = providerDataColumnRepository.findByIdProviderDataColumnNmIn(tableColumnList);

        // 4. 모든 요소가 존재하는지 확인
        List<String> foundExpns = foundColumns.stream()
                .map(column -> column.getId().getProviderDataColumnNm())
                .collect(Collectors.toList());

        if (foundExpns.size() != tableColumnList.size()) {
            // 존재하지 않는 요소 찾기
            List<String> missingExpns = tableColumnList.stream()
                    .filter(expn -> !foundExpns.contains(expn))
                    .collect(Collectors.toList());
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("프로바이더 데이터 컬럼 내역에 존재하지 않는 데이터 컬럼명이 있습니다. %s", missingExpns));
        }

        return ComponentInfoRes.of(componentInfoRepository.save(ComponentInfo.of(componentInfoReq)));
    }

    /**
     * 프로바이더 컴포넌트 설명 데이터 생성
     * 
     * @param componentInfoReq
     * @return
     */
    @Transactional
    public ComponentInfoRes createProviderComponentInfo(ComponentInfoReq.Insert componentInfoReq) {
        return ComponentInfoRes.of(componentInfoRepository.save(ComponentInfo.of(componentInfoReq)));
    }


    /**
     * 프로바이더 데이터 컬럼 생성
     * 
     * @param providerDataColumnsReq 프로바이더 데이터 컬럼 생성 데이터
     * @param providerNo 프로바이더 번호
     */
    @Transactional
    public List<ProviderDataColumnRes> createProviderDataColumn(int providerNo, List<ProviderDataColumnReq> providerDataColumnsReq) {

        Provider provider = providerRepository.findByProviderNoAndIsDeletedFalse(providerNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("provider no: %d", providerNo)));

        List<ProviderDataColumn> providerDataColumns = providerDataColumnsReq.stream()
                .map(providerDataColumn -> ProviderDataColumn.of(provider, providerDataColumn.providerDataColumnNm(), providerDataColumn.dataColumnExpn()))
                .collect(Collectors.toList());

        List<ProviderDataColumn> savedProviderDataColumns = providerDataColumnRepository.saveAll(providerDataColumns);

        return savedProviderDataColumns.stream()
                .map(ProviderDataColumnRes::of)
                .collect(Collectors.toList());
    }
}
