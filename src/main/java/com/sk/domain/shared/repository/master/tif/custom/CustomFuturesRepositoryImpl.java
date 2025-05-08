package com.sk.domain.shared.repository.master.tif.custom;

import static com.sk.domain.shared.entity.master.tif.QFutures.futures;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.entity.master.tif.Futures;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomFuturesRepositoryImpl implements CustomFuturesRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Map<LocalDateTime, List<Futures>> findByCodesAfterDate(List<String> codes, LocalDateTime lastDate) {
		List<Futures> futuresList = queryFactory
				.selectFrom(futures)
				.where(
						futures.code.in(codes)
								.and(futures.isDeleted.isFalse())
								.and(lastDate == null ? null : futures.date.gt(lastDate)))
				.orderBy(futures.date.asc())
				.fetch();

		return futuresList.stream()
				.collect(Collectors.groupingBy(
						Futures::getDate,
						TreeMap::new,
						Collectors.toList()));
	}
}
