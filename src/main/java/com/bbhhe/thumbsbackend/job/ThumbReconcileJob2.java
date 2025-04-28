package com.bbhhe.thumbsbackend.job;

import cn.hutool.core.collection.CollUtil;
import com.bbhhe.thumbsbackend.constant.ThumbConstant;
import com.bbhhe.thumbsbackend.listener.thumb.msg.ThumbEvent;
import com.bbhhe.thumbsbackend.model.entity.Thumb;
import com.bbhhe.thumbsbackend.service.ThumbService;
import com.google.common.collect.Sets;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对账任务实现
 */
@Service
@Slf4j
public class ThumbReconcileJob2 {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ThumbService thumbService;


    @Autowired
    private PulsarTemplate pulsarTemplate;

    @PostConstruct
    public void init() {
        log.info("应用启动，执行一次补偿任务");
        run(); // 调用 run 方法
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        log.info("开始进行数据对账");
        processUserKeys();
    }

    private void processUserKeys() {
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(ThumbConstant.USER_THUMB_KEY_PREFIX + "*")
                .count(100)
                .build();

        Cursor<String> cursor = redisTemplate.scan(scanOptions);
        List<Long> userBatch = new ArrayList<>();

        while (cursor.hasNext()) {
            String thumbKey = cursor.next();
            String[] parts = thumbKey.split(":");
            Long userId = Long.parseLong(parts[1]);
            userBatch.add(userId);

            // 每积累100个用户处理一次
            if (userBatch.size() >= 100) {
                processBatch(userBatch);
                userBatch.clear();
            }
        }

        // 处理剩余的用户
        if (!userBatch.isEmpty()) {
            processBatch(userBatch);
        }

        try {
            cursor.close();
        } catch (Exception e) {
            log.error("关闭cursor失败", e);
        }
    }

    private void processBatch(List<Long> users) {
        for (Long userId : users) {
            String key = ThumbConstant.USER_THUMB_KEY_PREFIX + userId;
            Set<String> redisBlogIds = redisTemplate.opsForHash().keys(key)
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());

            if (CollUtil.isEmpty(redisBlogIds)) {
                continue;
            }

            Set<String> dbBlogIds = thumbService.lambdaQuery()
                    .eq(Thumb::getUserId, userId)
                    .in(Thumb::getBlogId, redisBlogIds)
                    .list()
                    .stream()
                    .map(thumb -> thumb.getBlogId().toString())
                    .collect(Collectors.toSet());

            // 使用Guava Sets计算差集
            Set<Long> diffSet = Sets.difference(redisBlogIds, dbBlogIds)
                    .stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());

            if (!diffSet.isEmpty()) {
                sendCompensationEvents(userId, diffSet);
            }
        }
    }

    /**
     * 发送补偿事件到Pulsar
     */
    private void sendCompensationEvents(Long userId, Set<Long> blogIds) {
        blogIds.forEach(blogId -> {
            ThumbEvent thumbEvent = new ThumbEvent(userId, blogId, ThumbEvent.EventType.INCR, LocalDateTime.now());
            pulsarTemplate.sendAsync("thumb-topic", thumbEvent)
                    .exceptionally(ex -> {
                        log.error("补偿事件发送失败: userId={}, blogId={}", userId, blogId, ex);
                        return null;
                    });
        });
    }

}
