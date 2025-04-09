package com.sk.domain.shared.service.auth;

import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.service.MiUserService;
import com.sk.domain.shared.service.AuthorizationService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class UserPermissionStrategy implements PermissionStrategy {

    private final MiUserService userService;
    private final AuthorizationService authService;

    @Override
    public boolean canAccessApi() {
        return true;
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        Integer userNo = authService.getUserNo();
        PermissionRes permissionRes = userService.getUserGoodsPermissions(userNo);

        return findGoodsAuthority(permissionRes, goodsNo)
            .map(PermissionRes.GoodsAuthRes::isViewable)
            .orElse(false);
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        Integer userNo = authService.getUserNo();
        PermissionRes permissionRes = userService.getUserGoodsPermissions(userNo);

        return findGoodsAuthority(permissionRes, goodsNo)
            .map(PermissionRes.GoodsAuthRes::isDownloadable)
            .orElse(false);
    }

    /**
     * 상품 번호에 해당하는 권한 객체를 찾는다.
     * - 권한 방식이 '회사권한'이면 회사 기준으로
     * - '개별권한'이면 사용자 기준으로
     */
    private Optional<PermissionRes.GoodsAuthRes> findGoodsAuthority(PermissionRes res, Integer goodsNo) {
        return res.companies().stream()
            .flatMap(company -> company.providers().stream())
            .flatMap(provider -> provider.goods().stream())
            .filter(goods -> goods.goodsNo().equals(goodsNo))
            .findFirst();
    }

}

