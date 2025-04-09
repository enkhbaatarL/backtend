package com.sk.domain.shared.service.auth;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdminPermissionStrategy implements PermissionStrategy {

    @Override
    public boolean canAccessApi() {
        return true;
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        return false;
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        return false;
    }

}

