package com.sk.domain.mi.dto.response;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.CustomerCompanyScreenElementMapping;
import com.sk.domain.shared.entity.master.tmd.ScreenElement;
import com.sk.domain.shared.enums.ScreenElementType;
import com.sk.domain.shared.enums.news.MarketCategoryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
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

    // deprecated -> screen element로 대체
    @Schema(description = "정성모델 표시여부")
    Boolean isQualitative,

    // deprecated -> screen element로 대체
    @Schema(description = "마리 표시여부")
    Boolean isMari,

    // deprecated -> screen element로 대체
    @Schema(description = "미래 기능 예시 표출 여부")
    Boolean isFuture,

    @Schema(description = "뉴스시장카테고리코드", example = "CROIL")
    MarketCategoryCode newsMarketCategoryCode,

    // deprecated -> screen element로 대체
    @Schema(description = "뉴스레터 화면 표출 여부")
    Boolean isNewsletter,

    @Schema(description = "메뉴 화면요소 트리 (MENU_TOP / MENU_GROUP / MENU_ITEM)")
    List<ScreenElementNode> menus,

    @Schema(description = "플로팅 화면요소 (FLOATING)")
    List<ScreenElementNode> floating,

    @Schema(description = "기본 랜딩 화면요소 코드")
    String defaultLandingCd,

    @Schema(description = "회사 로고 이미지 URL, 회사 전용 로고가 없으면 null",
        example = "https://dev-mi.sk.com/images/sk-mi-d-osb-03/CPNYLOGO/3f2a8c1e-0d44-4b7b-9a11-2c5e7f6d8a90.png")
    String logoUrl

) {

    @Builder
    public MiCompanyDetailRes(Integer customerCompanyNo, String customerCompanyNm, String customerCompanyCd, String customerCompanyMipUrl, Boolean isQualitative, Boolean isMari, Boolean isFuture, Boolean isNewsletter, List<ScreenElementNode> menus, List<ScreenElementNode> floating, String defaultLandingCd) {
        this(customerCompanyNo, customerCompanyNm, customerCompanyCd, customerCompanyMipUrl, isQualitative, isMari, isFuture, null, isNewsletter, menus, floating, defaultLandingCd, null);
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
        ScreenElementType typeCd,

        @Schema(description = "하위 화면요소")
        List<ScreenElementNode> children
    ) {
        public ScreenElementNode {
            children = children == null ? List.of() : children;
        }
    }

    public record ScreenElementTrees(
        List<ScreenElementNode> menus,
        List<ScreenElementNode> floating
    ) {
        public ScreenElementTrees {
            menus = menus == null ? List.of() : menus;
            floating = floating == null ? List.of() : floating;
        }

        public String findFirstMenuItemCd() {
            return menus.stream()
                .flatMap(node -> findFirstMenuItem(node).stream())
                .findFirst()
                .orElse(null);
        }

        public boolean containsMenuItemCd(String screenElementCd) {
            if (screenElementCd == null) return false;
            return menus.stream()
                .anyMatch(node -> containsMenuItem(node, screenElementCd));
        }

        private static Optional<String> findFirstMenuItem(ScreenElementNode node) {
            if (ScreenElementType.MENU_ITEM == node.typeCd()) return Optional.of(node.screenElementCd());
            return node.children().stream()
                .flatMap(child -> findFirstMenuItem(child).stream())
                .findFirst();
        }

        private static boolean containsMenuItem(ScreenElementNode node, String screenElementCd) {
            if (ScreenElementType.MENU_ITEM == node.typeCd()) {
                return screenElementCd.equals(node.screenElementCd());
            }
            return node.children().stream()
                .anyMatch(child -> containsMenuItem(child, screenElementCd));
        }
    }

    public static class TreeBuilder {

        private final List<ScreenElement> screenElements;
        private final Map<Integer, CustomerCompanyScreenElementMapping> mappingByScreenElementNo;
        private final Map<Integer, List<ScreenElement>> childrenByParentNo;
        private final Predicate<ScreenElement> availabilityFilter;

        public TreeBuilder(List<ScreenElement> screenElements, List<CustomerCompanyScreenElementMapping> mappings) {
            this(screenElements, mappings, screenElement -> true);
        }

        public TreeBuilder(List<ScreenElement> screenElements,
            List<CustomerCompanyScreenElementMapping> mappings,
            Predicate<ScreenElement> availabilityFilter) {

            List<ScreenElement> safeScreenElements = screenElements == null ? List.of() : screenElements;
            List<CustomerCompanyScreenElementMapping> safeMappings = mappings == null ? List.of() : mappings;

            this.screenElements = safeScreenElements.stream()
                .filter(Objects::nonNull)
                .filter(se -> Boolean.TRUE.equals(se.getIsUsed()))
                .toList();

            this.availabilityFilter = availabilityFilter == null ? screenElement -> true : availabilityFilter;

            this.mappingByScreenElementNo = safeMappings.stream()
                .filter(Objects::nonNull)
                .filter(m -> m.getScreenElement() != null)
                .filter(m -> m.getScreenElement().getScreenElementNo() != null)
                .collect(Collectors.toMap(
                    m -> m.getScreenElement().getScreenElementNo(),
                    Function.identity(),
                    (left, right) -> left));

            this.childrenByParentNo = this.screenElements.stream()
                .filter(se -> se.getUpperScreenElement() != null)
                .filter(se -> se.getUpperScreenElement().getScreenElementNo() != null)
                .collect(Collectors.groupingBy(se -> se.getUpperScreenElement().getScreenElementNo()));

        }

        public ScreenElementTrees build() {

            List<ScreenElement> roots = screenElements.stream()
                .filter(se -> se.getUpperScreenElement() == null)
                .toList();

            List<ScreenElementNode> menus = roots.stream()
                .filter(se -> se.getScreenElementTypeCd() != null && se.getScreenElementTypeCd().isMenu())
                .sorted(comparator())
                .map(this::toNode)
                .filter(Objects::nonNull)
                .toList();

            List<ScreenElementNode> floating = roots.stream()
                .filter(se -> se.getScreenElementTypeCd() != null && se.getScreenElementTypeCd().isFloating())
                .sorted(comparator())
                .map(this::toNode)
                .filter(Objects::nonNull)
                .toList();

            return new ScreenElementTrees(menus, floating);
        }

        private ScreenElementNode toNode(ScreenElement screenElement) {

            if (!resolveExposure(screenElement)) return null;
            if (!availabilityFilter.test(screenElement)) return null;

            ScreenElementType screenElementType = screenElement.getScreenElementTypeCd();
            if (screenElementType == null) return null;

            List<ScreenElementNode> children = childrenByParentNo
                .getOrDefault(screenElement.getScreenElementNo(), List.of())
                .stream()
                .sorted(comparator())
                .map(this::toNode)
                .filter(Objects::nonNull)
                .toList();

            // 하위 메뉴가 모두 제외된 메뉴 그룹은 빈 껍데기만 남지 않도록 제거한다.(MENU_TOP은 자식이 없어도 단독 메뉴로 노출될 수 있음)
            if (screenElementType == ScreenElementType.MENU_GROUP && children.isEmpty()) {
                return null;
            }

            return ScreenElementNode.builder()
                .screenElementCd(screenElement.getScreenElementCd())
                .typeCd(screenElementType)
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
