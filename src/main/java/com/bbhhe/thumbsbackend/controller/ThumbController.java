package com.bbhhe.thumbsbackend.controller;

import com.bbhhe.thumbsbackend.common.BaseResponse;
import com.bbhhe.thumbsbackend.common.ResultUtils;
import com.bbhhe.thumbsbackend.model.dto.thumb.DoThumbRequest;
import com.bbhhe.thumbsbackend.model.entity.User;
import com.bbhhe.thumbsbackend.service.ThumbService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/thumb")
public class ThumbController {
    @Autowired
    @Qualifier("thumbServiceMq")
    private ThumbService thumbService;

    private final Counter successCounter;
    private final Counter failureCounter;

    public ThumbController(MeterRegistry registry) {
        this.successCounter = Counter.builder("thumb.success.count")
                .description("Total successful thumb")
                .register(registry);
        this.failureCounter = Counter.builder("thumb.failure.count")
                .description("Total failed thumb")
                .register(registry);
    }


    @PostMapping("/do")
    public BaseResponse<Boolean> doThumb(@RequestBody DoThumbRequest request, HttpServletRequest servletRequest) throws InterruptedException {
        if (request == null || request.getBlogId() == null) {
            failureCounter.increment();
            return ResultUtils.error(40000, "请求参数错误");
        }
        HttpSession session = servletRequest.getSession();
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            failureCounter.increment();
            return ResultUtils.error(40100, "用户未登录");
        }
        boolean result = thumbService.doThumb(request.getBlogId(), loginUser.getId());
        if(result){
            successCounter.increment();
        }else {
            failureCounter.increment();
        }
        return ResultUtils.success(result);
    }

    @PostMapping("/undo")
    public BaseResponse<Boolean> undoThumb(@RequestBody DoThumbRequest request, HttpServletRequest servletRequest) throws InterruptedException {
        if (request == null || request.getBlogId() == null) {
            failureCounter.increment();
            return ResultUtils.error(40000, "请求参数错误");
        }
        HttpSession session = servletRequest.getSession();
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            failureCounter.increment();
            return ResultUtils.error(40100, "用户未登录");
        }
        boolean result = thumbService.undoThumb(request.getBlogId(), loginUser.getId());
        if(result){
            successCounter.increment();
        }else {
            failureCounter.increment();
        }
        return ResultUtils.success(result);
    }
}