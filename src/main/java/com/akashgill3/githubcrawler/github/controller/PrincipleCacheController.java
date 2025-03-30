package com.akashgill3.githubcrawler.github.controller;


import com.akashgill3.githubcrawler.github.service.PrincipleCache;
import com.akashgill3.githubcrawler.github.model.Principle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PrincipleCacheController {

    private final PrincipleCache principleCache;

    public PrincipleCacheController(PrincipleCache principleCache) {
        this.principleCache = principleCache;
    }

    @GetMapping("/principles")
    public Map<String, Principle> getPrinciples() {
        return principleCache.getAll();
    }

    @GetMapping("/principles/{name}")
    public Principle getPrincipleByName(@PathVariable String name) {
        return principleCache.get(name);
    }
}

