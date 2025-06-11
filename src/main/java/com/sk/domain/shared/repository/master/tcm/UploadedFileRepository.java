package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tcm.UploadedFile;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    @Query(value = """
            SELECT u
            FROM UploadedFile u
            WHERE u.isDeleted = false
                AND u.fileNo = :fileNo
                AND (u.fileValidEndDt IS NULL OR u.fileValidEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))})
                """)
    Optional<UploadedFile> findByFileNo(@Param("fileNo") Long fileNo);

    @Query(value = """
            SELECT u
            FROM UploadedFile u
            WHERE u.isDeleted = false
                AND u.fileNo IN (:fileNoList)
                AND (u.fileValidEndDt IS NULL OR u.fileValidEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))})
                """)
    List<UploadedFile> findByFileNoList(@Param("fileNoList") List<Long> fileNoList);

    @Query(value = """
            SELECT u
            FROM UploadedFile u
            WHERE u.isDeleted = false
                AND u.storagePathNm = :filePath
                AND (u.fileValidEndDt IS NULL OR u.fileValidEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))})
                """)
    Optional<UploadedFile> findByFilePath(@Param("filePath") String filePath);

    @Query(value = """
            SELECT u
            FROM UploadedFile u
            WHERE u.isDeleted = false
                AND u.storagePathNm IN (:filePathList)
                AND (u.fileValidEndDt IS NULL OR u.fileValidEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))})
                """)
    List<UploadedFile> findByFilePathList(@Param("filePathList") List<String> filePathList);

    @Query(value = """
            SELECT u
            FROM UploadedFile u
            WHERE u.isDeleted = false
                AND u.fileRelationshipObjectDistinguishNo = :fileRelationshipObjectDistinguishNo
                AND u.fileRelationshipObjectSeparateCd = :fileRelationshipObjectSeparateCode
                AND (u.fileValidEndDt IS NULL OR u.fileValidEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))})
                """)
    List<UploadedFile> findByRelationshipObjectInfo(
            @Param("fileRelationshipObjectDistinguishNo") Integer fileRelationshipObjectDistinguishNo,
            @Param("fileRelationshipObjectSeparateCode") String fileRelationshipObjectSeparateCode);

    Optional<UploadedFile> findByFileNoAndIsDeletedFalse(Long fileNo);

    @Query(value = """
            SELECT u
            FROM UploadedFile u
            WHERE u.isDeleted = false
                AND (u.isImageConversionCompleted != true OR u.isImageConversionCompleted IS NULL)
                AND u.fileExtNm = :fileExtNm
                AND u.fileRelationshipObjectSeparateCd = :fileRelationshipObjectSeparateCd
                AND (u.fileValidEndDt IS NULL OR u.fileValidEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))})
            """)
    List<UploadedFile> findPdfToImagesTarget(@Param("fileExtNm") String fileExtNm,
            @Param("fileRelationshipObjectSeparateCd") String fileRelationshipObjectSeparateCd);

    @Query(value = """
            SELECT u
            FROM UploadedFile u
            WHERE u.isDeleted = false
                AND u.fileRelationshipObjectDistinguishNo = :fileRelationshipObjectDistinguishNo
                AND u.fileRelationshipObjectSeparateCd = :fileRelationshipObjectSeparateCode
                AND (u.fileValidEndDt IS NULL OR u.fileValidEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))})
                """)
    UploadedFile findByFileRelationshipObjectDistinguishNoAndFileRelationshipObjectSeparateCode(
            @Param("fileRelationshipObjectDistinguishNo") Integer fileRelationshipObjectDistinguishNo,
            @Param("fileRelationshipObjectSeparateCode") String fileRelationshipObjectSeparateCode);

    @Query(value = """
            SELECT u
            FROM UploadedFile u
            WHERE u.fileRelationshipObjectDistinguishNo = :fileRelationshipObjectDistinguishNo
                AND u.isDeleted = false
                AND (u.fileValidEndDt IS NULL OR u.fileValidEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))})
            """)
    List<UploadedFile> findByFileRelationshipObjectDistinguishNoAndIsDeletedFalse(
            @Param("fileRelationshipObjectDistinguishNo") Integer fileRelationshipObjectDistinguishNo);

    @Query(value = """
            SELECT u
            FROM UploadedFile u
            WHERE u.originalFileNm = :fileName
                AND u.fileHashVal = :fileHash
                AND u.isDeleted = false
                AND (u.fileValidEndDt IS NULL OR u.fileValidEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))})
            """)
    List<UploadedFile> findByFileNameAndFileHash(@Param("fileName") String fileName, @Param("fileHash") String fileHash);

    List<UploadedFile> findByFileRelationshipObjectDistinguishNoInAndIsDeletedFalse(List<Integer> goodsDataNos);

}
