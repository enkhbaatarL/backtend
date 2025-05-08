package com.sk.domain.shared.repository.master.tif;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tif.Futures;
import com.sk.domain.shared.repository.master.tif.custom.CustomFuturesRepository;

public interface FuturesRepository extends JpaRepository<Futures, Long>, CustomFuturesRepository {
}
