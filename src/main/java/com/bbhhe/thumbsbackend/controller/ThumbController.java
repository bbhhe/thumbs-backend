package com.bbhhe.thumbsbackend.controller;

import com.bbhhe.thumbsbackend.common.BaseResponse;
import com.bbhhe.thumbsbackend.common.ResultUtils;
import com.bbhhe.thumbsbackend.model.dto.thumb.DoThumbRequest;
import com.bbhhe.thumbsbackend.model.entity.User;
import com.bbhhe.thumbsbackend.service.ThumbService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/thumb")
public class ThumbController {
    @Resource
    @Qualifier("thumbService")
    private ThumbService thumbService;

    @PostMapping("/do")
    public BaseResponse<Boolean> doThumb(@RequestBody DoThumbRequest request, HttpServletRequest servletRequest) {
        if (request == null || request.getBlogId() == null) {
            return ResultUtils.error(40000, "请求参数错误");
        }
        HttpSession session = servletRequest.getSession();
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResultUtils.error(40100, "用户未登录");
        }
        boolean result = thumbService.doThumb(request.getBlogId(), loginUser.getId());
        return ResultUtils.success(result);
    }

    @PostMapping("/undo")
    public BaseResponse<Boolean> undoThumb(@RequestBody DoThumbRequest request, HttpServletRequest servletRequest) {
        if (request == null || request.getBlogId() == null) {
            return ResultUtils.error(40000, "请求参数错误");
        }
        HttpSession session = servletRequest.getSession();
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResultUtils.error(40100, "用户未登录");
        }
        boolean result = thumbService.undoThumb(request.getBlogId(), loginUser.getId());
        return ResultUtils.success(result);
    }
}