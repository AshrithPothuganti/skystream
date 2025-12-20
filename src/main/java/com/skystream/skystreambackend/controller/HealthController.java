package com.skystream.skystreambackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "SkyStream backend running on Render";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
 