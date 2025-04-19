package com.bbhhe.thumbsbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbhhe.thumbsbackend.mapper.UserMapper;
import com.bbhhe.thumbsbackend.model.entity.User;
import com.bbhhe.thumbsbackend.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}