package com.hhr.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller //必须这个注解
public class MainController {
    @RequestMapping("/main")
    public String a() {
        return "main";
    }
}
