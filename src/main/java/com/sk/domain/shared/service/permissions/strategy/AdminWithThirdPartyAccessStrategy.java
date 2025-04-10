package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.service.MiUserService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor
public class AdminWithThirdPartyAccessStrategy implements PermissionStrategy {

    private final MiUserService userService;
    private final AuthorizationService authorizationService;

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles).anyMatch(role -> role == Role.ADMIN) && authorizationService.isThirdPartyAccessAllowed();
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        return getGoodsAuth(goodsNo)
            .map(PermissionRes.GoodsAuthRes::isViewable)
            .orElse(false);
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        return getGoodsAuth(goodsNo)
            .map(PermissionRes.GoodsAuthRes::isDownloadable)
            .orElse(false);
    }

    private Optional<PermissionRes.GoodsAuthRes> getGoodsAuth(Integer goodsNo) {
        return userService.getAdminGoodsPermissions().companies().stream()
            .flatMap(company -> company.providers().stream())
            .flatMap(provider -> provider.goods().stream())
            .filter(goods -> goods.goodsNo().equals(goodsNo))
            .findFirst();
    }

}


