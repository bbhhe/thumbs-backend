package com.bbhhe.thumbsbackend.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbhhe.thumbsbackend.constant.RedisScriptConstant;
import com.bbhhe.thumbsbackend.exception.ErrorCode;
import com.bbhhe.thumbsbackend.exception.ThrowUtils;
import com.bbhhe.thumbsbackend.mapper.ThumbMapper;
import com.bbhhe.thumbsbackend.model.entity.Thumb;
import com.bbhhe.thumbsbackend.service.ThumbService;
import com.bbhhe.thumbsbackend.utils.RedisKeyUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


@Service("thumbServiceRedis")
public class ThumbServiceRedisImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean undoThumb(Long blogId, Long userId) {
        Long result = redisTemplate.execute(RedisScriptConstant.UNTHUMB_SCRIPT,
                ListUtil.toList(RedisKeyUtil.getTempThumbKey(getTimeSlice()),
                        RedisKeyUtil.getUserThumbKey(userId)), userId, blogId);

        ThrowUtils.throwIf(-1==result, ErrorCode.PARAMS_ERROR,"用户未点赞");

        return 1 == result;
    }

    @Override
    public boolean doThumb(Long blogId, Long userId) {

        Long result = redisTemplate.execute(RedisScriptConstant.THUMB_SCRIPT,
                ListUtil.toList(RedisKeyUtil.getTempThumbKey(getTimeSlice()),
                        RedisKeyUtil.getUserThumbKey(userId)), userId, blogId);

        ThrowUtils.throwIf(-1==result, ErrorCode.PARAMS_ERROR,"用户已经点赞");

        return 1 == result;
    }

    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        // 获取到当前时间前最近的整数秒，比如当前 11:20:23 ，获取到 11:20:20
        return DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        // 直接检查Redis中的点赞记录
        return redisTemplate.opsForHash().hasKey(RedisKeyUtil.getUserThumbKey(userId), blogId.toString());
    }

}