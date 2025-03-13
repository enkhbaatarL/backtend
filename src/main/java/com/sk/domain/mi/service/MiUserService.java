package com.sk.domain.mi.service;

import com.sk.domain.mi.dto.response.GoodsAuthorityRes;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.repository.master.tam.AuthManageGoodsMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiUserService {

    private final AuthManageGoodsMappingRepository authManageGoodsMappingRepository;

    /**
     * 사용자 로그인 ID를 통해 사용자의 상품권한(VIEW, DOWNLOAD) 목록을 조회한다.
     *
     * @param userLoginId 사용자 로그인 ID
     * @return List<GoodsAuthorityRes>
     */
    public List<GoodsAuthorityRes> getUserGoodsAuthorities(String userLoginId) {
        List<AuthManageGoodsMapping> goodsMappings = authManageGoodsMappingRepository.getUserGoodsAuthoritiesByUserLoginId(userLoginId);
        return GoodsAuthorityRes.fromAuthorityMappings(goodsMappings);
    }

}
