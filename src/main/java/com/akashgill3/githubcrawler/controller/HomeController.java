package com.akashgill3.githubcrawler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@CrossOrigin(maxAge = 3600)
public class HomeController {

    @GetMapping
    public String home() {
        return "home";
    }
}
