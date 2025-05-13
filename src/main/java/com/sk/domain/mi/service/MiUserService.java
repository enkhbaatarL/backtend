package com.sk.domain.mi.service;

import com.sk.common.config.redis.CacheType;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.repository.master.tam.AuthManageGoodsMappingRepository;
import com.sk.domain.shared.service.CacheService;
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

    private final CacheService cacheService;
    private final AuthManageGoodsMappingRepository authManageGoodsMappingRepository;

    /**
     * 사용자 번호를 통해 사용자의 상품권한(VIEW, DOWNLOAD) 목록을 조회한다.
     *
     * @param userNo 사용자 번호
     * @return List<GoodsAuthorityRes>
     */
    // @Cacheable(
    //     cacheNames = CacheType.PERMISSIONS_CONSTANT,
    //     key = "#userNo",
    //     unless = "#result.companies == null || #result.companies.empty"
    // )
    public PermissionRes getUserGoodsPermissions(Integer userNo) {
        List<AuthManageGoodsMapping> goodsMappings = authManageGoodsMappingRepository.findUserGoodsPermissions(userNo);

        PermissionRes permissionRes = PermissionRes.fromAuthorityMappings(goodsMappings);

        // 역방향 인덱스 업데이트 (캐시 로드 시 호출)
        cacheService.updateReverseIndices(userNo, permissionRes);

        return permissionRes;
    }

    // todo 캐시
    public PermissionRes getAdminGoodsPermissions() {
        List<AuthManageGoodsMapping> goodsMappings = authManageGoodsMappingRepository.findAdminGoodsPermissions();
        return PermissionRes.fromAuthorityMappings(goodsMappings);
    }

    // todo 캐시
    public PermissionRes getPlusOperatorGoodsPermissions(Integer companyNo) {
        List<AuthManageGoodsMapping> goodsMappings = authManageGoodsMappingRepository.findPlusOperatorGoodsPermissions(companyNo);
        return PermissionRes.fromAuthorityMappings(goodsMappings);
    }

}
