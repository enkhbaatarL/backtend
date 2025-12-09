package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsEventCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsEventCategoryRepository extends JpaRepository<NewsEventCategory, Long> {

    /**
     * Root Code로 카테고리 조회
     */
    @Query("SELECT nec FROM NewsEventCategory nec " +
           "WHERE nec.rootCode = :rootCode")
    List<NewsEventCategory> findByRootCode(@Param("rootCode") Integer rootCode);

    /**
     * Quad Class로 카테고리 조회
     */
    @Query("SELECT nec FROM NewsEventCategory nec " +
           "WHERE nec.quadClass = :quadClass")
    List<NewsEventCategory> findByQuadClass(@Param("quadClass") Integer quadClass);

    /**
     * Root Code와 Quad Class로 카테고리 조회
     */
    @Query("SELECT nec FROM NewsEventCategory nec " +
           "WHERE nec.rootCode = :rootCode AND nec.quadClass = :quadClass")
    Optional<NewsEventCategory> findByRootCodeAndQuadClass(
        @Param("rootCode") Integer rootCode,
        @Param("quadClass") Integer quadClass
    );

    /**
     * Nature(성격)로 카테고리 조회
     */
    @Query("SELECT nec FROM NewsEventCategory nec " +
           "WHERE nec.nature = :nature")
    List<NewsEventCategory> findByNature(@Param("nature") String nature);
}
