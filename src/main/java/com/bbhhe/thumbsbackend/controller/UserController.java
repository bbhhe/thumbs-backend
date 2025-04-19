package com.bbhhe.thumbsbackend.controller;

import com.bbhhe.thumbsbackend.model.entity.User;
import com.bbhhe.thumbsbackend.service.UserService;
import com.bbhhe.thumbsbackend.common.BaseResponse;
import com.bbhhe.thumbsbackend.common.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    
    @GetMapping("/login")
    public BaseResponse<User> login(@RequestParam Long userId, HttpServletRequest request) {
        User user = userService.getById(userId);
        if (user != null) {
            HttpSession session = request.getSession();
            session.setAttribute("loginUser", user);
            return ResultUtils.success(user);
        }
        return ResultUtils.error(40100, "用户不存在");
    }
    
    @GetMapping("/get/login")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return ResultUtils.error(40100, "用户未登录");
        }
        return ResultUtils.success(user);
    }
}