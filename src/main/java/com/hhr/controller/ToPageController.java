package com.hhr.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller //必须这个注解
@RequestMapping("/src/main/resources/templates")
public class ToPageController {
    /**
     * 跳转页面
     * @param url
     * @return
     */
    @RequestMapping("/toPage")
    public String toPage(@RequestParam(value = "url") String url){
        return url;
    }

    @RequestMapping("/main")
    public String a() {
        return "main";
    }
}
