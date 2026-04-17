package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.MiCompanyDetailRes;
import com.sk.domain.mi.dto.response.MiCompanyRes;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.CustomerCompanyMarketCategory;
import com.sk.domain.shared.entity.master.tmd.CustomerCompanyScreenElementMapping;
import com.sk.domain.shared.entity.master.tmd.ScreenElement;
import com.sk.domain.shared.enums.ScreenElementCode;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyMarketCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyScreenElementMappingRepository;
import com.sk.domain.shared.repository.master.tmd.ScreenElementRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiCompanyService {

    private final CustomerCompanyRepository customerCompanyRepository;
    private final CustomerCompanyMarketCategoryRepository customerCompanyMarketCategoryRepository;
    private final ScreenElementRepository screenElementRepository;
    private final CustomerCompanyScreenElementMappingRepository customerCompanyScreenElementMappingRepository;
    private final MiForecastService miForecastService;

    /**
     * 고객사 회사 목록 조회
     *
     * @return 고객사 회사 목록
     */
    public List<MiCompanyRes> getCompanyList(String requestHost) {
        return customerCompanyRepository.findAllMiCompanies(requestHost);
    }

    /**
     * 고객사 상세 정보와 화면요소 메뉴 트리를 조회한다.
     * 회사별 화면요소 노출 설정을 반영하고, Forecast 데이터가 없는 회사는 `AI Forecast Model`과 `Signal Map` 메뉴를 제외한다.
     * 메뉴 필터링 이후 기본 랜딩 메뉴가 노출 대상에서 제외된 경우에는 실제 노출 가능한 첫 번째 메뉴로 보정한다.
     *
     * @param companyNo 고객회사번호
     * @param requestHost 요청 호스트
     * @return 고객사 상세 정보
     */
    public MiCompanyDetailRes getCompanyDetail(Integer companyNo, String requestHost) {
        MiCompanyDetailRes miCompanyDetailRes = customerCompanyRepository.findMiCompany(companyNo, requestHost);
        CustomerCompanyMarketCategory customerCompanyMarketCategory = customerCompanyMarketCategoryRepository.findFirstByCustomerCompanyNoAndIsDeletedFalse(
            companyNo).orElse(null);

        CustomerCompany customerCompany = customerCompanyRepository
            .findActiveByCustomerCompanyNo(companyNo, DateUtil.toYmd(LocalDate.now()))
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "company not found: companyNo=%d".formatted(companyNo)));

        List<ScreenElement> screenElements = screenElementRepository.findByIsDeletedFalseOrderByDefaultOrderNoAscScreenElementNoAsc();
        List<CustomerCompanyScreenElementMapping> mappings = customerCompanyScreenElementMappingRepository.findByCustomerCompanyAndIsDeletedFalse(customerCompany);

        boolean forecastDataExists = miForecastService.forecastDataExists(companyNo);
        MiCompanyDetailRes.ScreenElementTrees trees = new MiCompanyDetailRes.TreeBuilder(
            screenElements,
            mappings,
            screenElement -> isAvailableScreenElement(screenElement, forecastDataExists)
        ).build();

        String defaultLandingCd = resolveDefaultLandingCd(customerCompany, trees);

        MiCompanyDetailRes.MiCompanyDetailResBuilder builder = miCompanyDetailRes.toBuilder()
            .menus(trees.menus())
            .floating(trees.floating())
            .defaultLandingCd(defaultLandingCd);

        if (customerCompanyMarketCategory != null) {
            builder.newsMarketCategoryCode(customerCompanyMarketCategory.getNwsMrktCtgrCd());
        }

        return builder.build();
    }

    private boolean isAvailableScreenElement(ScreenElement screenElement, boolean forecastDataExists) {
        // Forecast 데이터가 없는 회사는 Forecast 모델과 Signal Map 메뉴를 노출하지 않는다.
        return forecastDataExists || !ScreenElementCode.requiresForecastData(screenElement.getScreenElementCd());
    }

    private String resolveDefaultLandingCd(CustomerCompany customerCompany, MiCompanyDetailRes.ScreenElementTrees trees) {
        String defaultLandingCd = customerCompany.getDefaultLandingScreenElement() != null
            ? customerCompany.getDefaultLandingScreenElement().getScreenElementCd()
            : null;

        // 기본 랜딩 메뉴가 필터링되어 사라졌다면 실제 노출 가능한 첫 번째 메뉴로 보정한다.
        if (trees.containsMenuItemCd(defaultLandingCd)) {
            return defaultLandingCd;
        }
        return trees.findFirstMenuItemCd();
    }

}
