package com.sk.domain.shared.entity.master.tcm;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "tcm_filel")
@Comment("TCM_파일")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadedFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_no", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("파일번호")
    private Long fileNo;

    @Column(name = "org_file_nm", nullable = false, length = 300)
    @Comment("원본파일명")
    private String originalFileNm;

    @Column(name = "strg_file_nm", nullable = false, length = 300)
    @Comment("저장파일명")
    private String storageFileNm;

    @Column(name = "strg_path_nm", nullable = true, length = 255)
    @Comment("저장경로명")
    private String storagePathNm;

    @Column(name = "file_size", columnDefinition = "int unsigned")
    @Comment("파일사이즈")
    private Long fileSize;

    @Column(name = "filex_nm", length = 20)
    @Comment("파일확장자명")
    private String fileExtNm;

    @Column(name = "mime_type_nm", length = 100)
    @Comment("MIME유형명")
    private String mimeTypeNm;

    @Column(name = "file_vald_end_dt", length = 8)
    @Comment("파일유효종료일자")
    private String fileValidEndDt;

    @Column(name = "file_rel_obj_dstg_no", columnDefinition = "int unsigned")
    @Comment("파일관계객체식별번호")
    private Integer fileRelationshipObjectDistinguishNo;

    @Column(name = "file_rel_obj_spt_cd", length = 20)
    @Comment("파일관계객체구분코드")
    private String fileRelationshipObjectSeparateCd;

    @Column(name = "file_page_cnt", columnDefinition = "mediumint unsigned")
    @Comment("파일페이지수")
    private Integer filePageCnt;

    @Column(name = "img_conv_cmpt_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("이미지변환완료여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isImageConversionCompleted;

    @Column(name = "file_hash_val", length = 20)
    @Comment("파일해시값")
    private String fileHashVal;

    @Builder
    public UploadedFile(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Long fileNo, String originalFileNm, String storageFileNm, String storagePathNm, Long fileSize,
            String fileExtNm, String mimeTypeNm, String fileValidEndDt, Integer fileRelationshipObjectDistinguishNo,
            String fileRelationshipObjectSeparateCd, Integer filePageCnt, Boolean isImageConversionCompleted, String fileHashVal) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.fileNo = fileNo;
        this.originalFileNm = originalFileNm;
        this.storageFileNm = storageFileNm;
        this.storagePathNm = storagePathNm;
        this.fileSize = fileSize;
        this.fileExtNm = fileExtNm;
        this.mimeTypeNm = mimeTypeNm;
        this.fileValidEndDt = fileValidEndDt;
        this.fileRelationshipObjectDistinguishNo = fileRelationshipObjectDistinguishNo;
        this.fileRelationshipObjectSeparateCd = fileRelationshipObjectSeparateCd;
        this.filePageCnt = filePageCnt;
        this.isImageConversionCompleted = isImageConversionCompleted;
        this.fileHashVal = fileHashVal;
    }

    public static UploadedFile of(String originalFileNm, String storageFileNm, String storagePathNm, long fileSize, String fileExtNm,
            String mimeTypeNm,
            String fileValidEndDt, Integer fileRelationshipObjectDistinguishNo, String fileRelationshipObjectSeparateCd, String fileHashVal) {
        return UploadedFile.builder()
                .originalFileNm(originalFileNm)
                .storageFileNm(storageFileNm)
                .storagePathNm(storagePathNm)
                .fileSize(fileSize)
                .fileExtNm(fileExtNm)
                .mimeTypeNm(mimeTypeNm)
                .fileValidEndDt(fileValidEndDt)
                .fileRelationshipObjectDistinguishNo(fileRelationshipObjectDistinguishNo)
                .fileRelationshipObjectSeparateCd(fileRelationshipObjectSeparateCd)
                .isImageConversionCompleted(Boolean.FALSE)
                .fileHashVal(fileHashVal)
                .build();
    }

    public void update(String storagePathNm, Integer fileRelationshipObjectDistinguishNo, String fileRelationshipObjectSeparateCd,
            String fileValidEndDt) {
        this.storagePathNm = storagePathNm;
        this.fileRelationshipObjectDistinguishNo = fileRelationshipObjectDistinguishNo;
        this.fileRelationshipObjectSeparateCd = fileRelationshipObjectSeparateCd;
        this.fileValidEndDt = fileValidEndDt;
    }

    public void attachToRelationship(
            Integer fileRelationshipObjectDistinguishNo,
            String fileRelationshipObjectSeparateCd,
            String fileValidEndDt) {
        this.fileRelationshipObjectDistinguishNo = fileRelationshipObjectDistinguishNo;
        this.fileRelationshipObjectSeparateCd = fileRelationshipObjectSeparateCd;
        this.fileValidEndDt = fileValidEndDt;
    }

    public void updateUploadedImagesResult(int numberOfPages) {
        this.filePageCnt = numberOfPages;
        this.isImageConversionCompleted = Boolean.TRUE;
    }

}
