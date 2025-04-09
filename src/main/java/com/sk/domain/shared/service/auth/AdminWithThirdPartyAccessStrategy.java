package com.sk.domain.shared.service.auth;

import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.service.MiUserService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class AdminWithThirdPartyAccessStrategy implements PermissionStrategy {

    private final MiUserService userService;

    @Override
    public boolean canAccessApi() {
        return true;
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


