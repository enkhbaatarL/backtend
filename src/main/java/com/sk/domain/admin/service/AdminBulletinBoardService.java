package com.sk.domain.admin.service;

import org.springframework.data.domain.Pageable;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.provider.GoodsUpdateReq;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.response.provider.GoodsDataSourceRes;
import com.sk.domain.admin.dto.response.provider.GoodsDeleteRes;
import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.admin.dto.response.provider.GoodsUpdateRes;
import com.sk.domain.admin.dto.response.provider.ProviderCreateRes;
import com.sk.domain.admin.dto.response.provider.ProviderDeleteRes;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.admin.dto.response.provider.ProviderUpdateRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.repository.master.tcm.BulletinBoardRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.repository.master.tmd.ProviderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminBulletinBoardService {
  private final BulletinBoardRepository bulletinBoardRepository;
}
