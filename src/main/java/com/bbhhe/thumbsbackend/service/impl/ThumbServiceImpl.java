package com.bbhhe.thumbsbackend.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbhhe.thumbsbackend.constant.ThumbConstant;
import com.bbhhe.thumbsbackend.exception.BusinessException;
import com.bbhhe.thumbsbackend.exception.ErrorCode;
import com.bbhhe.thumbsbackend.manager.cache.CacheManager;
import com.bbhhe.thumbsbackend.mapper.BlogMapper;
import com.bbhhe.thumbsbackend.mapper.ThumbMapper;
import com.bbhhe.thumbsbackend.model.entity.Blog;
import com.bbhhe.thumbsbackend.model.entity.Thumb;
import com.bbhhe.thumbsbackend.service.ThumbService;
import jakarta.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.locks.ReentrantLock;


@Service("thumbServicelocal")
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private CacheManager cacheManager;
    @Autowired
    private UserServiceImpl userServiceImpl;

    @Override
    public boolean undoThumb(Long blogId, Long userId) {
        if (!lock.tryLock()) {
            return false;
        }
        try {
            return transactionTemplate.execute(status -> {
                // 1. 判断是否已点赞
//                if (!hasThumb(blogId, userId)) {
//                    return false;
//                }
                Object thumbIdObj = cacheManager.get(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
                if (thumbIdObj == null || thumbIdObj.equals(ThumbConstant.UN_THUMB_CONSTANT)) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"用户未点赞");
                }

                // 2. 更新博客点赞数
                Blog blog = blogMapper.selectById(blogId);
                if (blog == null) {
                    return false;
                }
                blog.setThumbCount(blog.getThumbCount() - 1);
                blogMapper.updateById(blog);

                // 3. 删除点赞记录
                LambdaQueryWrapper<Thumb> queryWrapper = new LambdaQueryWrapper<Thumb>()
                        .eq(Thumb::getBlogId, blogId)
                        .eq(Thumb::getUserId, userId);
                boolean success = this.remove(queryWrapper);
                // 点赞记录从 Redis 删除
                if (success) {
                    String hashKey = ThumbConstant.USER_THUMB_KEY_PREFIX +userId;
                    String fieldKey = blogId.toString();
                    redisTemplate.opsForHash().delete(hashKey, fieldKey);
                    cacheManager.putIfPresent(hashKey, fieldKey, ThumbConstant.UN_THUMB_CONSTANT);
                }
                return success;
            });
        } finally {
            lock.unlock();
        }
    }
    @Resource
    private BlogMapper blogMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public boolean doThumb(Long blogId, Long userId) {
        if (!lock.tryLock()) {
            return false;
        }
        try {
            return transactionTemplate.execute(status -> {
                // 1. 判断是否已点赞
                if (hasThumb(blogId, userId)) {
                    return false;
                }

                // 2. 更新博客点赞数
                Blog blog = blogMapper.selectById(blogId);
                if (blog == null) {
                    return false;
                }
                blog.setThumbCount(blog.getThumbCount() + 1);
                blogMapper.updateById(blog);

                // 3. 保存点赞记录
                Thumb thumb = new Thumb();
                thumb.setBlogId(blogId);
                thumb.setUserId(userId);
                boolean success = this.save(thumb);
                // 点赞记录存入 Redis
                if (success) {
                    String hashKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
                    String fieldKey = blogId.toString();
                    Long realThumbId = thumb.getId();
                    redisTemplate.opsForHash().put(hashKey, fieldKey, realThumbId);
                    cacheManager.putIfPresent(hashKey, fieldKey, realThumbId);
                }
                return success;
            });
        } finally {
            lock.unlock();
        }
    }

    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        // 获取到当前时间前最近的整数秒，比如当前 11:20:23 ，获取到 11:20:20
        return DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;
    }

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        Object thumbIdObj = cacheManager.get(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
        if (thumbIdObj == null) {
            return false;
        }
        Long thumbId = (Long) thumbIdObj;
        return !thumbId.equals(ThumbConstant.UN_THUMB_CONSTANT);
    }

}