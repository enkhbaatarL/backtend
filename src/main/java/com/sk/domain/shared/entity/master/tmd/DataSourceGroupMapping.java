package com.sk.domain.shared.entity.master.tmd;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmd.DataSourceGroupMappingId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "tmd_datasrcgrpmappl")
@Comment("TMD_데이터소스그룹매핑내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataSourceGroupMapping extends BaseEntity {

    @EmbeddedId
    private DataSourceGroupMappingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mnagt_gds_data_src_no", insertable = false, updatable = false)
    @Comment("주체상품데이터소스번호")
    private GoodsDataSourceDetail mainGoodsDataSourceDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cmprs_gds_data_src_no", insertable = false, updatable = false)
    @Comment("비교상품데이터소스번호")
    private GoodsDataSourceDetail compareGoodsDataSourceDetail;

    @Column(name = "yaxis_sprt_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("Y축분리여부")
    @ColumnDefault("'Y'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isYAxisSeparated;

    @Builder
    public DataSourceGroupMapping(GoodsDataSourceDetail main, GoodsDataSourceDetail compare, Boolean isYAxisSeparated) {
        this.id = new DataSourceGroupMappingId(main.getGoodsDataSourceNo(), compare.getGoodsDataSourceNo());
        this.mainGoodsDataSourceDetail = main;
        this.compareGoodsDataSourceDetail = compare;
        this.isYAxisSeparated = isYAxisSeparated;
    }

}
