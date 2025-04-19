package com.bbhhe.thumbsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbhhe.thumbsbackend.mapper.BlogMapper;
import com.bbhhe.thumbsbackend.mapper.ThumbMapper;
import com.bbhhe.thumbsbackend.model.entity.Blog;
import com.bbhhe.thumbsbackend.model.entity.Thumb;
import com.bbhhe.thumbsbackend.service.ThumbService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {
    @Override
    public boolean undoThumb(Long blogId, Long userId) {
        if (!lock.tryLock()) {
            return false;
        }
        try {
            return transactionTemplate.execute(status -> {
                // 1. 判断是否已点赞
                LambdaQueryWrapper<Thumb> queryWrapper = new LambdaQueryWrapper<Thumb>()
                        .eq(Thumb::getBlogId, blogId)
                        .eq(Thumb::getUserId, userId);
                long count = this.count(queryWrapper);
                if (count == 0) {
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
                return this.remove(queryWrapper);
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
                LambdaQueryWrapper<Thumb> queryWrapper = new LambdaQueryWrapper<Thumb>()
                        .eq(Thumb::getBlogId, blogId)
                        .eq(Thumb::getUserId, userId);
                long count = this.count(queryWrapper);
                if (count > 0) {
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
                return this.save(thumb);
            });
        } finally {
            lock.unlock();
        }
    }
}