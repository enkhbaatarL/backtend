package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsTopic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsTopicRepository extends JpaRepository<NewsTopic, Long> {

    @Query("SELECT nt FROM NewsTopic nt " +
           "WHERE nt.isDeleted = false " +
           "AND nt.useYn = :useYn " +
           "AND (nt.userRegisteredYn = 'N' OR (nt.userRegisteredYn = 'Y' AND nt.creatorUserNo = :userNo)) " +
           "ORDER BY nt.newsTopicId ASC")
    List<NewsTopic> findActiveDefaultAndUserTopics(
        @Param("useYn") String useYn,
        @Param("userNo") int userNo
    );

    List<NewsTopic> findByIsDeletedFalseAndUseYnOrderByNewsTopicIdAsc(String useYn);
}
