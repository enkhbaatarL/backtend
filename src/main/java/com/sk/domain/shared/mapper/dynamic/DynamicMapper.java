package com.sk.domain.shared.mapper.dynamic;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DynamicMapper {
    List<Map<String, Object>> selectDynamic(Map<String, Object> params);
}
