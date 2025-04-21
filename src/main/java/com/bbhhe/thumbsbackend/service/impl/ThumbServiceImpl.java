package com.bbhhe.thumbsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbhhe.thumbsbackend.constant.ThumbConstant;
import com.bbhhe.thumbsbackend.mapper.BlogMapper;
import com.bbhhe.thumbsbackend.mapper.ThumbMapper;
import com.bbhhe.thumbsbackend.model.dto.thumb.ThumbInfo;
import com.bbhhe.thumbsbackend.model.entity.Blog;
import com.bbhhe.thumbsbackend.model.entity.Thumb;
import com.bbhhe.thumbsbackend.service.ThumbService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {
    @Resource
    private BlogMapper blogMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Lazy
    @Resource
    private ThumbService thumbService;

    private final ReentrantLock lock = new ReentrantLock();

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean undoThumb(Long blogId, Long userId) {
        if (!lock.tryLock()) {
            return false;
        }
        try {
            return transactionTemplate.execute(status -> {
                // 1. 判断是否已点赞
                if (!hasThumb(blogId, userId)) {
                    return false;
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
                if (success) {
                    // 4. 从Redis中删除点赞记录
                    redisTemplate.opsForHash().delete(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
                }
                return success;
            });
        } finally {
            lock.unlock();
        }
    }

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
                if (success) {
                    // 4. 判断是否为热数据（发布时间 ≤ 30天）
                    LocalDateTime createTime = blog.getCreateTime();
                    boolean isHotData = Duration.between(createTime, LocalDateTime.now()).toDays() <= ThumbConstant.HOT_DATA_DAYS;
                    
                    if (isHotData) {
                        // 只有热数据才存入Redis
                        ThumbInfo thumbInfo = new ThumbInfo();
                        thumbInfo.setId(thumb.getId());
                        thumbInfo.setExpireTime(System.currentTimeMillis() + ThumbConstant.HOT_DATA_DAYS * 24 * 60 * 60 * 1000L);
                        redisTemplate.opsForHash().put(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString(), thumbInfo);
                    }
                }
                return success;
            });
        } finally {
            lock.unlock();
        }
    }


    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        // 1. 优先查询Redis
        String redisKey = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
        String blogIdStr = blogId.toString();
        Object thumbInfoObj = redisTemplate.opsForHash().get(redisKey, blogIdStr);
        
        if (thumbInfoObj != null) {
            ThumbInfo thumbInfo = (ThumbInfo) thumbInfoObj;
            if (System.currentTimeMillis() <= thumbInfo.getExpireTime()) {
                return true;
            } else {
                // 已过期，删除Redis中的记录
                redisTemplate.opsForHash().delete(redisKey, blogIdStr);
            }
        }

        // 2. Redis未命中或已过期，查询MySQL
        Long count = thumbService.lambdaQuery()
                .eq(Thumb::getUserId, userId)
                .eq(Thumb::getBlogId, blogId)
                .count();
        
        return count > 0;
    }

}