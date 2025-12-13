package com.skystream.skystreambackend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "dashboard";
    }
    
    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    @GetMapping("/calendar")
    public String calendar() {
        return "calendar";
    }

    @GetMapping("/ten-days")
    public String tenDays() {
        return "ten-days";
    }

    @GetMapping("/country")
    public String country() {
        return "country";
    }
}
