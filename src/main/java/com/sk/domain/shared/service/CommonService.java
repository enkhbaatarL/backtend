package com.sk.domain.shared.service;

import com.sk.domain.shared.dto.response.common.UserDetailRes;
import com.sk.domain.shared.entity.master.tmd.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommonService {

    public UserDetailRes getUserDetail(User user) {
        return UserDetailRes.of(user);
    }

}
