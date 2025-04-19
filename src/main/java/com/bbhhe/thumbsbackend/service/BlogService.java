package com.bbhhe.thumbsbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bbhhe.thumbsbackend.model.entity.Blog;
import com.bbhhe.thumbsbackend.model.vo.BlogVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface BlogService extends IService<Blog> {
    BlogVO getBlogVoById(Long blogId, HttpServletRequest request);
    
    List<BlogVO> getBlogVoList(List<Blog> blogList, HttpServletRequest request);
}