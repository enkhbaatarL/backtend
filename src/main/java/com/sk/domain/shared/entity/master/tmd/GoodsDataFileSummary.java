package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_상품데이터파일요약")
@Table(name = "tmd_gdsdatam_file_smry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsDataFileSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gds_data_smry_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("상품데이터요약번호")
    private Integer goodsDataSummaryNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_no", referencedColumnName = "file_no", nullable = false)
    @Comment("파일번호")
    private UploadedFile file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_no", referencedColumnName = "gds_data_no", nullable = false)
    @Comment("상품데이터번호")
    private GoodsData goodsData;

    @Column(name = "file_smry", columnDefinition = "mediumtext")
    @Comment("상품데이터 파일 요약")
    private String fileSummary;

    @Column(name = "file_smry_shrt", columnDefinition = "text")
    @Comment("상품데이터 파일 요약 요약")
    private String fileSummaryShort;

    @Column(name = "file_rcrd", columnDefinition = "json")
    @Comment("상품데이터 파일 레코드")
    private String fileRecord;

}
