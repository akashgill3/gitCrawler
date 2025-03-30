package com.akashgill3.githubcrawler.github.model;

import java.util.Map;

public record Principle(
        String content,
        PrincipleMetadata metadata,
        Map<String, Practise> practises) implements GitHubNode<PrincipleMetadata> {
}
