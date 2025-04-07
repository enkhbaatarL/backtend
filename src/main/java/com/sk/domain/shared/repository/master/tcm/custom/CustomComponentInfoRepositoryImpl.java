package com.sk.domain.shared.repository.master.tcm.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomComponentInfoRepositoryImpl implements CustomComponentInfoRepository {

    private final JPAQueryFactory queryFactory;

}
