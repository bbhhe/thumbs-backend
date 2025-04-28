package com.bbhhe.thumbsbackend.job;

import cn.hutool.core.collection.CollUtil;
import com.bbhhe.thumbsbackend.constant.ThumbConstant;
import com.bbhhe.thumbsbackend.listener.thumb.msg.ThumbEvent;
import com.bbhhe.thumbsbackend.model.entity.Thumb;
import com.bbhhe.thumbsbackend.service.ThumbService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 对账任务实现
 */
//@Service
@Slf4j
public class ThumbReconcileJob {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ThumbService thumbService;


    @Autowired
    private PulsarTemplate pulsarTemplate;

//    @PostConstruct
//    public void init() {
//        log.info("应用启动，执行一次补偿任务");
//        run(); // 调用 run 方法
//    }

    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        log.info("开始进行数据对账");
        Set<String> thumbKeys = redisTemplate.keys(ThumbConstant.USER_THUMB_KEY_PREFIX+"*");

        List<Long> users = new ArrayList<>();
        for (String thumbKey : thumbKeys) {
            String[] parts = thumbKey.split(":");
            Long userId = Long.parseLong(parts[1]);
            users.add(userId);
        }


        for (Long userId : users) {
            String key = ThumbConstant.USER_THUMB_KEY_PREFIX+userId;
            Set blogIds = redisTemplate.opsForHash().keys(key);

            Set<Long> diffSet = new HashSet<>();
            for (Object blogId : blogIds) {
                List<Thumb> list = thumbService.lambdaQuery().eq(Thumb::getUserId, userId)
                        .eq(Thumb::getBlogId, blogId)
                        .list();
                if(CollUtil.isEmpty(list)){
                    diffSet.add(Long.parseLong((String) blogId));
                }
            }

            sendCompensationEvents(userId,diffSet);
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
