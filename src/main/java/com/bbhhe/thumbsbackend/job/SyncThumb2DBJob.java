package com.bbhhe.thumbsbackend.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbhhe.thumbsbackend.mapper.BlogMapper;
import com.bbhhe.thumbsbackend.model.entity.Thumb;
import com.bbhhe.thumbsbackend.service.ThumbService;
import com.bbhhe.thumbsbackend.utils.RedisKeyUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 定时将Redis中的临时点赞数据同步到数据库
 */
@Component
@Slf4j
public class SyncThumb2DBJob {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    @Qualifier("thumbServiceRedis")
    private ThumbService thumbService;

    @Resource
    private BlogMapper blogMapper;

    @Scheduled(fixedRate = 10000) // 每10秒执行一次
    @Transactional(rollbackFor = Exception.class)
    public void syncThumbData() {
        log.info("开始执行");
        DateTime nowDate = DateUtil.date();
        // 如果秒数为0~9 则回到上一分钟的50秒
        int second = (DateUtil.second(nowDate) / 10 - 1) * 10;
        if (second == -10) {
            second = 50;
            // 回到上一分钟
            nowDate = DateUtil.offsetMinute(nowDate, -1);
        }
        String date = DateUtil.format(nowDate, "HH:mm:") + second;
        processTimeSliceData(date);
        log.info("临时数据同步完成");
    }

    public void processTimeSliceData(String date) {
        try {
            String key = RedisKeyUtil.getTempThumbKey(date);

            // 获取该时间片的所有点赞数据
            Map<Object, Object> thumbData = redisTemplate.opsForHash().entries(key);
            if (CollUtil.isEmpty(thumbData)) {
                return;
            }

            // 统计每个博客的点赞数变化
            Map<Long, Long> blogThumbCountMap = new HashMap<>();
            List<Thumb> thumbList = new ArrayList<>();
            LambdaQueryWrapper<Thumb> wrapper = new LambdaQueryWrapper<>();
            boolean needRemove = false;
            for (Object userIdBlogIdObj : thumbData.keySet()) {
                String hashKey = (String) userIdBlogIdObj;
                String[] parts = hashKey.split(":");
                Long userId = Long.parseLong(parts[0]);
                Long blogId = Long.parseLong(parts[1]);

                // -1 取消点赞，1 点赞
                Integer thumbType = Integer.valueOf(thumbData.get(hashKey).toString());
                if(thumbType == 1){
                    Thumb thumb = new Thumb();
                    thumb.setUserId(userId);
                    thumb.setBlogId(blogId);
                    thumbList.add(thumb);
                }else if(-1 == thumbType){
                    needRemove = true;
                    //批量删除
                    wrapper.or().eq(Thumb::getUserId,userId).eq(Thumb::getBlogId,blogId);
                }else {
                    if (thumbType != 0){
                        log.warn("数据异常：{}",userId+","+blogId+","+thumbType);
                    }
                    continue;
                }

                // 计算点赞增量
                blogThumbCountMap.put(blogId, blogThumbCountMap.getOrDefault(blogId, 0L) + thumbType);
            }


            // 批量更新博客点赞数
            if (!blogThumbCountMap.isEmpty()) {
                blogMapper.batchUpdateThumbCount(blogThumbCountMap);
            }

            // 批量保存点赞记录
            if (!thumbList.isEmpty()) {
                thumbService.saveBatch(thumbList);
            }

            if(needRemove){
                thumbService.remove(wrapper);
            }

            Thread.startVirtualThread(() -> {
                // 删除已处理的Redis数据
                redisTemplate.delete(key);
            });


            log.info("成功同步时间片{}的点赞数据，处理了{}条记录", date, thumbData.size());
        } catch (Exception e) {
            log.error("处理时间片{}的数据时发生异常", date, e);
            throw e;
        }
    }
}
