package com.bbhhe.thumbsbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog")
public class Blog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long userId;
    private String title;
    private String coverImg;
    private String content;
    private Integer thumbCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}