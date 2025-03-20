package com.sk.domain.mi.service;

import com.sk.common.config.redis.Cache;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.repository.master.tam.AuthManageGoodsMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
     * 사용자 번호를 통해 사용자의 상품권한(VIEW, DOWNLOAD) 목록을 조회한다.
     *
     * @param userNo 사용자 번호
     * @return List<GoodsAuthorityRes>
     */
    @Cacheable(cacheNames = Cache.PERMISSIONS_CONSTANT, key = "#userNo")
    public PermissionRes getUserGoodsPermissions(Integer userNo) {
        List<AuthManageGoodsMapping> goodsMappings = authManageGoodsMappingRepository.findUserGoodsPermissions(userNo);
        return PermissionRes.fromAuthorityMappings(goodsMappings);
    }

}
