package com.sk.domain.shared.repository.master.tif.custom;

import static com.sk.domain.shared.entity.master.tif.QFutures.futures;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.entity.master.tif.Futures;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomFuturesRepositoryImpl implements CustomFuturesRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<LocalDateTime, List<Futures>> findPendingByCodes(List<String> codes) {

        List<Futures> futuresList = queryFactory
                .selectFrom(futures)
                .where(futures.isModifiedLoad.eq(false)
                        .and(futures.code.in(codes)))
                .orderBy(futures.date.asc()) // date 기준 오름차순 정렬
                .fetch();

        return futuresList.stream()
                .collect(Collectors.groupingBy(Futures::getDate));
    }
}
