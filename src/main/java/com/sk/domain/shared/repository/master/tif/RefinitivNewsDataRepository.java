package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.shared.entity.compositeKey.tif.RefinitivNewsDataId;
import com.sk.domain.shared.entity.master.tif.RefinitivNewsData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefinitivNewsDataRepository extends JpaRepository<RefinitivNewsData, RefinitivNewsDataId> {

}
