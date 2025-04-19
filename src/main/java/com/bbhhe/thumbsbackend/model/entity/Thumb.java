package com.bbhhe.thumbsbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("thumb")
public class Thumb {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long userId;
    private Long blogId;
    private LocalDateTime createTime;
}