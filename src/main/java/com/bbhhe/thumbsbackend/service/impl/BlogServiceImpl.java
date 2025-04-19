package com.bbhhe.thumbsbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bbhhe.thumbsbackend.mapper.BlogMapper;
import com.bbhhe.thumbsbackend.model.entity.Blog;
import com.bbhhe.thumbsbackend.model.entity.Thumb;
import com.bbhhe.thumbsbackend.model.entity.User;
import com.bbhhe.thumbsbackend.model.vo.BlogVO;
import com.bbhhe.thumbsbackend.service.BlogService;
import com.bbhhe.thumbsbackend.service.ThumbService;
import com.bbhhe.thumbsbackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.List;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {
    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private ThumbService thumbService;

    @Override
    public BlogVO getBlogVoById(Long blogId, HttpServletRequest request) {
        // 获取博客信息
        Blog blog = this.getById(blogId);
        if (blog == null) {
            return null;
        }

        // 获取当前登录用户
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return null;
        }

        // 转换为VO对象
        BlogVO blogVO = new BlogVO();
        BeanUtils.copyProperties(blog, blogVO);

        // 查询用户是否点赞
        Long userId = loginUser.getId();
        Long count = thumbService.lambdaQuery()
                .eq(Thumb::getUserId, userId)
                .eq(Thumb::getBlogId, blogId)
                .count();
        blogVO.setHasThumb(count > 0);

        return blogVO;
    }

    @Override
    public List<BlogVO> getBlogVoList(List<Blog> blogList, HttpServletRequest request) {
        // 获取当前登录用户
        HttpSession session = request.getSession();
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return null;
        }

        // 转换博客列表为VO对象
        List<BlogVO> blogVOList = blogList.stream().map(blog -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);

            // 查询用户是否点赞
            Long userId = loginUser.getId();
            Long count = thumbService.lambdaQuery()
                    .eq(Thumb::getUserId, userId)
                    .eq(Thumb::getBlogId, blog.getId())
                    .count();
            blogVO.setHasThumb(count > 0);

            return blogVO;
        }).toList();

        return blogVOList;
    }
}