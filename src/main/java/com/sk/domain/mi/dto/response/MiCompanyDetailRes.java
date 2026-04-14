package com.sk.domain.mi.dto.response;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.CustomerCompanyScreenElementMapping;
import com.sk.domain.shared.entity.master.tmd.ScreenElement;
import com.sk.domain.shared.enums.news.MarketCategoryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder(toBuilder = true)
public record MiCompanyDetailRes(

    @Schema(description = "고객회사번호")
    Integer customerCompanyNo,

    @Schema(description = "고객회사명")
    String customerCompanyNm,

    @Schema(description = "고객회사코드")
    String customerCompanyCd,

    @Schema(description = "고객회사MI플랫폼URL주소")
    String customerCompanyMipUrl,

    @Schema(description = "정성모델 표시여부")
    Boolean isQualitative,

    @Schema(description = "마리 표시여부")
    Boolean isMari,

    @Schema(description = "미래 기능 예시 표출 여부")
    Boolean isFuture,

    @Schema(description = "뉴스시장카테고리코드", example = "CROIL")
    MarketCategoryCode newsMarketCategoryCode,

    @Schema(description = "뉴스레터 화면 표출 여부")
    Boolean isNewsletter,

    @Schema(description = "메뉴 화면요소 트리 (MENU_TOP / MENU_GROUP / MENU_ITEM)")
    List<ScreenElementNode> menus,

    @Schema(description = "플로팅 화면요소 (FLOATING)")
    List<ScreenElementNode> floating

) {

    @Builder
    public MiCompanyDetailRes(Integer customerCompanyNo, String customerCompanyNm, String customerCompanyCd, String customerCompanyMipUrl, Boolean isQualitative, Boolean isMari, Boolean isFuture, Boolean isNewsletter, List<ScreenElementNode> menus, List<ScreenElementNode> floating) {
        this(customerCompanyNo, customerCompanyNm, customerCompanyCd, customerCompanyMipUrl, isQualitative, isMari, isFuture, null, isNewsletter, menus, floating);
    }

    public static MiCompanyDetailRes of(CustomerCompany customerCompany, String miDefaultDomain,
        String requestHost, String configMipUrl) {
        return MiCompanyDetailRes.builder()
            .customerCompanyNo(customerCompany.getCustomerCompanyNo())
            .customerCompanyNm(customerCompany.getCustomerCompanyNm())
            .customerCompanyCd(customerCompany.getCustomerCompanyCd())
            .customerCompanyMipUrl(customerCompany.getDynamicMipUrl(requestHost, miDefaultDomain, configMipUrl))
            .isQualitative(customerCompany.getIsQualitative())
            .isMari(customerCompany.getIsMari())
            .isFuture(customerCompany.getIsFuture())
            .isNewsletter(customerCompany.getIsNewsLetter())
            .build();
    }

    @Builder
    public record ScreenElementNode(

        @Schema(description = "화면요소코드")
        String screenElementCd,

        @Schema(description = "화면요소유형코드")
        String typeCd,

        @Schema(description = "하위 화면요소")
        List<ScreenElementNode> children
    ) {
    }

    public record ScreenElementTrees(
        List<ScreenElementNode> menus,
        List<ScreenElementNode> floating
    ) {
    }

    public static class TreeBuilder {

        private final List<ScreenElement> screenElements;
        private final Map<Integer, CustomerCompanyScreenElementMapping> mappingByScreenElementNo;
        private final Map<Integer, List<ScreenElement>> childrenByParentNo;

        public TreeBuilder(List<ScreenElement> screenElements, List<CustomerCompanyScreenElementMapping> mappings) {

            this.screenElements = screenElements.stream()
                .filter(se -> Boolean.TRUE.equals(se.getIsUsed()))
                .toList();

            this.mappingByScreenElementNo = mappings.stream()
                .collect(Collectors.toMap(
                    m -> m.getScreenElement().getScreenElementNo(),
                    Function.identity(),
                    (left, right) -> left));

            this.childrenByParentNo = this.screenElements.stream()
                .filter(se -> se.getUpperScreenElement() != null)
                .collect(Collectors.groupingBy(se -> se.getUpperScreenElement().getScreenElementNo()));

        }

        public ScreenElementTrees build() {

            List<ScreenElement> roots = screenElements.stream()
                .filter(se -> se.getUpperScreenElement() == null)
                .toList();

            List<ScreenElementNode> menus = roots.stream()
                .filter(se -> se.getScreenElementTypeCd().isMenu())
                .sorted(comparator())
                .map(this::toNode)
                .filter(Objects::nonNull)
                .toList();

            List<ScreenElementNode> floating = roots.stream()
                .filter(se -> se.getScreenElementTypeCd().isFloating())
                .sorted(comparator())
                .map(this::toNode)
                .filter(Objects::nonNull)
                .toList();

            return new ScreenElementTrees(menus, floating);
        }

        private ScreenElementNode toNode(ScreenElement screenElement) {

            if (!resolveExposure(screenElement)) return null;

            List<ScreenElementNode> children = childrenByParentNo
                .getOrDefault(screenElement.getScreenElementNo(), List.of())
                .stream()
                .sorted(comparator())
                .map(this::toNode)
                .filter(Objects::nonNull)
                .toList();

            return ScreenElementNode.builder()
                .screenElementCd(screenElement.getScreenElementCd())
                .typeCd(screenElement.getScreenElementTypeCd().name())
                .children(children)
                .build();
        }

        private boolean resolveExposure(ScreenElement screenElement) {
            CustomerCompanyScreenElementMapping mapping = mappingByScreenElementNo.get(screenElement.getScreenElementNo());
            if (mapping != null) return Boolean.TRUE.equals(mapping.getIsExposed());
            return Boolean.TRUE.equals(screenElement.getIsDefaultExposed());
        }

        private Integer resolveOrderNo(ScreenElement screenElement) {
            CustomerCompanyScreenElementMapping mapping = mappingByScreenElementNo.get(screenElement.getScreenElementNo());
            if (mapping != null && mapping.getOrderNo() != null) return mapping.getOrderNo();
            return screenElement.getDefaultOrderNo();
        }

        private Comparator<ScreenElement> comparator() {
            return Comparator
                .comparing(this::resolveOrderNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ScreenElement::getScreenElementNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(se -> Objects.toString(se.getScreenElementCd(), ""));
        }
    }

}
