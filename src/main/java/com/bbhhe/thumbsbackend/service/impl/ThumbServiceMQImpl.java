package com.bbhhe.thumbsbackend.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbhhe.thumbsbackend.constant.RedisScriptConstant;
import com.bbhhe.thumbsbackend.exception.ErrorCode;
import com.bbhhe.thumbsbackend.exception.ThrowUtils;
import com.bbhhe.thumbsbackend.listener.thumb.msg.ThumbEvent;
import com.bbhhe.thumbsbackend.mapper.ThumbMapper;
import com.bbhhe.thumbsbackend.model.entity.Thumb;
import com.bbhhe.thumbsbackend.service.ThumbService;
import com.bbhhe.thumbsbackend.utils.RedisKeyUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Primary
@Service("thumbServiceMq")
@Slf4j
public class ThumbServiceMQImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private PulsarTemplate<ThumbEvent> pulsarTemplate;

    @Override
    public boolean undoThumb(Long blogId, Long userId) {
        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId);
        //执行lua脚本
        Long result = redisTemplate.execute(RedisScriptConstant.UNTHUMB_SCRIPT_MQ,
                ListUtil.toList(RedisKeyUtil.getUserThumbKey(userId)), blogId);

        ThrowUtils.throwIf(-1==result, ErrorCode.PARAMS_ERROR,"用户未点赞");

        ThumbEvent thumbEvent = ThumbEvent.builder().userId(userId).blogId(blogId)
                .eventTime(LocalDateTime.now()).type(ThumbEvent.EventType.DECR).build();

        pulsarTemplate.sendAsync("thumb-topic", thumbEvent).exceptionally(ex -> {
            redisTemplate.opsForHash().put(userThumbKey, blogId.toString(), true);
            log.error("点赞事件发送失败: userId={}, blogId={}", userId, blogId, ex);
            return null;
        });

        return 1 == result;
    }

    @Override
    public boolean doThumb(Long blogId, Long userId) {
        //执行lua脚本
        String userThumbKey = RedisKeyUtil.getUserThumbKey(userId);
        Long result = redisTemplate.execute(RedisScriptConstant.THUMB_SCRIPT_MQ,
                ListUtil.toList(userThumbKey), blogId);

        ThrowUtils.throwIf(-1==result, ErrorCode.PARAMS_ERROR,"用户已经点赞");

        ThumbEvent thumbEvent = ThumbEvent.builder().userId(userId).blogId(blogId)
                .eventTime(LocalDateTime.now()).type(ThumbEvent.EventType.INCR).build();

        pulsarTemplate.sendAsync("thumb-topic", thumbEvent).exceptionally(ex -> {
            redisTemplate.opsForHash().delete(userThumbKey, blogId.toString(), true);
            log.error("点赞事件发送失败: userId={}, blogId={}", userId, blogId, ex);
            return null;
        });
        return true;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        // 直接检查Redis中的点赞记录
        return redisTemplate.opsForHash().hasKey(RedisKeyUtil.getUserThumbKey(userId), blogId.toString());
    }

}