package com.bbhhe.thumbsbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bbhhe.thumbsbackend.model.entity.Thumb;

public interface ThumbService extends IService<Thumb> {
    /**
     * 取消点赞
     * @param blogId 博客id
     * @param userId 用户id
     * @return 是否成功
     */
    boolean undoThumb(Long blogId, Long userId);
    /**
     * 执行点赞操作
     * @param blogId 博客ID
     * @param userId 用户ID
     * @return 是否点赞成功
     */
    boolean doThumb(Long blogId, Long userId);

    /**
     * 是否点赞
     * @param blogId 博客ID
     * @param userId 用户ID
     * @return 是否点赞
     */
    Boolean hasThumb(Long blogId, Long userId);
}