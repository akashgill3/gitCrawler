package com.akashgill3.githubcrawler.github.model;

import java.util.List;

public record PractiseMetadata(
        String name,
        String owner,
        String metrics,
        List<String> tags) implements Metadata {
}