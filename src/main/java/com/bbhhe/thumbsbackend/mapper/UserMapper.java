package com.bbhhe.thumbsbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbhhe.thumbsbackend.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}