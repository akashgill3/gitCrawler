package com.akashgill3.githubcrawler.github.model;

import java.util.Map;

public record Practise(
        String content,
        PractiseMetadata metadata,
        Map<String, Practise> subPractises) implements GitHubNode<PractiseMetadata> {
}