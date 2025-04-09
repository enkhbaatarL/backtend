package com.sk.domain.shared.service.auth;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OperatorPermissionStrategy implements PermissionStrategy {

    @Override
    public boolean canAccessApi() {
        return true;
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        // 운영자는 상품에 접근 권한 없음
        return false;
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        // 운영자는 다운로드 권한도 없음
        return false;
    }

}

