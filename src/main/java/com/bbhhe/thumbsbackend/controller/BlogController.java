package com.bbhhe.thumbsbackend.controller;

import com.bbhhe.thumbsbackend.model.entity.Blog;
import com.bbhhe.thumbsbackend.model.vo.BlogVO;
import com.bbhhe.thumbsbackend.service.BlogService;
import com.bbhhe.thumbsbackend.common.BaseResponse;
import com.bbhhe.thumbsbackend.common.ResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {
    @Autowired
    private BlogService blogService;

    @GetMapping("/get")
    public BaseResponse<BlogVO> getBlogVo(@RequestParam Long blogId, HttpServletRequest request) {
        BlogVO blogVO = blogService.getBlogVoById(blogId, request);
        if (blogVO == null) {
            return ResultUtils.error(40100, "获取博客信息失败");
        }
        return ResultUtils.success(blogVO);
    }

    @GetMapping("/list")
    public BaseResponse<List<BlogVO>> listBlogVo(HttpServletRequest request) {
        List<Blog> blogList = blogService.list();
        List<BlogVO> blogVOList = blogService.getBlogVoList(blogList, request);
        if (blogVOList == null) {
            return ResultUtils.error(40100, "获取博客列表失败");
        }
        return ResultUtils.success(blogVOList);
    }

}