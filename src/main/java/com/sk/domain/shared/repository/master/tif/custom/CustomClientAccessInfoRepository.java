package com.sk.domain.shared.repository.master.tif.custom;

import com.sk.domain.admin.dto.response.client.ClientAccessInfoRes;
import org.springframework.data.domain.Page;

public interface CustomClientAccessInfoRepository {

    Page<ClientAccessInfoRes> findClientInfoByConditions(int page, int size);

}
