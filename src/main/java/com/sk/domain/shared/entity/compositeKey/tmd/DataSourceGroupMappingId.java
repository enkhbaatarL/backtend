package com.sk.domain.shared.entity.compositeKey.tmd;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DataSourceGroupMappingId implements Serializable {

    @Column(name = "mnagt_gds_data_src_no", nullable = false, columnDefinition = "int unsigned")
    private Integer mainGoodsDataSourceNo;

    @Column(name = "cmprs_gds_data_src_no", nullable = false, columnDefinition = "int unsigned")
    private Integer compareGoodsDataSourceNo;

}

