package com.sk.domain.shared.repository.master.tam.custom;

import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;

import java.util.List;

public interface CustomAuthManageGoodsMapping {

    List<AuthManageGoodsMapping> getAuthority(String userLoginId);

}
