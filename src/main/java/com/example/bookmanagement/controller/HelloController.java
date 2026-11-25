package com.example.bookmanagement.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/api/hello")
    public String hello() {
        log.info("访问了图书管理系统欢迎接口");
        return "图书管理系统已启动！！！";
    }
}
