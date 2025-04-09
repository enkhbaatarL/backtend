package com.sk.domain.shared.service.auth;

public interface PermissionStrategy {

    boolean canAccessApi();

    boolean canViewGoods(Integer goodsNo);

    boolean canDownloadGoods(Integer goodsNo);

}
