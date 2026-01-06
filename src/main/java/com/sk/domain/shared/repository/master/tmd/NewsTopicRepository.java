package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsTopic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsTopicRepository extends JpaRepository<NewsTopic, Long> {

    List<NewsTopic> findByIsDeletedFalseAndUseYnOrderByNewsTopicIdAsc(String useYn);
}
