package com.sk.domain.shared.mapper.dynamic;

import com.sk.domain.shared.mapper.dto.SchemaMetadata;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface MetadataMapper {

    // todo 캐싱
    SchemaMetadata selectSchemaMetadata(Map<String, Object> params);

}
