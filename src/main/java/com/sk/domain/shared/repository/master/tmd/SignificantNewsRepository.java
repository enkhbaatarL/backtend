package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.SignificantNews;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignificantNewsRepository extends JpaRepository<SignificantNews, Long> {

    Optional<SignificantNews> findByNwsSigId(Long newsSigId);
}
