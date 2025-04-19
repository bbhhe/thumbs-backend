package com.bbhhe.thumbsbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbhhe.thumbsbackend.model.entity.Blog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogMapper extends BaseMapper<Blog> {
}