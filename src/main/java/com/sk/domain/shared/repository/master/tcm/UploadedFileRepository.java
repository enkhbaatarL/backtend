package com.sk.domain.shared.repository.master.tcm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tcm.UploadedFile;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long>, JpaSpecificationExecutor<UploadedFile> {

  @Query(value = """
      select u
      from UploadedFile u
      where u.isDeleted = false
      and u.fileNo = :fileNo
      and u.fileValidEndDt = :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
          """)
  Optional<UploadedFile> findByFileNo(@Param("fileNo") Long fileNo);

}
