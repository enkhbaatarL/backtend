package com.sk.domain.mi.service;

import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.MiCompanyDetailRes;
import com.sk.domain.mi.dto.response.MiCompanyRes;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.CustomerCompanyMarketCategory;
import com.sk.domain.shared.entity.master.tmd.CustomerCompanyScreenElementMapping;
import com.sk.domain.shared.entity.master.tmd.ScreenElement;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyMarketCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyScreenElementMappingRepository;
import com.sk.domain.shared.repository.master.tmd.ScreenElementRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    /**
     * 고객사 회사 목록 조회
     *
     * @return 고객사 회사 목록
     */
    public List<MiCompanyRes> getCompanyList(String requestHost) {
        return customerCompanyRepository.findAllMiCompanies(requestHost);
    }

    public MiCompanyDetailRes getCompanyDetail(Integer companyNo, String requestHost) {
        MiCompanyDetailRes miCompanyDetailRes = customerCompanyRepository.findMiCompany(companyNo, requestHost);
        CustomerCompanyMarketCategory customerCompanyMarketCategory = customerCompanyMarketCategoryRepository.findFirstByCustomerCompanyNoAndIsDeletedFalse(
            companyNo).orElse(null);

        CustomerCompany customerCompany = customerCompanyRepository
            .findActiveByCustomerCompanyNo(companyNo, DateUtil.toYmd(LocalDate.now()))
            .orElseThrow();

        List<ScreenElement> screenElements = screenElementRepository.findByIsDeletedFalseOrderByDefaultOrderNoAscScreenElementNoAsc();
        List<CustomerCompanyScreenElementMapping> mappings = customerCompanyScreenElementMappingRepository.findByCustomerCompanyAndIsDeletedFalse(customerCompany);

        MiCompanyDetailRes.ScreenElementTrees trees = new MiCompanyDetailRes.TreeBuilder(screenElements, mappings).build();

        String defaultLandingCd = customerCompany.getDefaultLandingScreenElement() != null
            ? customerCompany.getDefaultLandingScreenElement().getScreenElementCd()
            : findFirstMenuItemCd(trees.menus());

        miCompanyDetailRes = miCompanyDetailRes.toBuilder()
            .menus(trees.menus())
            .floating(trees.floating())
            .defaultLandingCd(defaultLandingCd)
            .build();

        if (customerCompanyMarketCategory != null) {
            miCompanyDetailRes = miCompanyDetailRes.toBuilder()
                .newsMarketCategoryCode(customerCompanyMarketCategory.getNwsMrktCtgrCd())
                .build();
        }

        return miCompanyDetailRes;
    }

    private String findFirstMenuItemCd(List<MiCompanyDetailRes.ScreenElementNode> menus) {
        return menus.stream()
            .flatMap(node -> findFirstMenuItem(node).stream())
            .findFirst()
            .orElse(null);
    }

    private Optional<String> findFirstMenuItem(MiCompanyDetailRes.ScreenElementNode node) {
        if ("MENU_ITEM".equals(node.typeCd())) return Optional.of(node.screenElementCd());
        return node.children().stream()
            .flatMap(child -> findFirstMenuItem(child).stream())
            .findFirst();
    }
}
