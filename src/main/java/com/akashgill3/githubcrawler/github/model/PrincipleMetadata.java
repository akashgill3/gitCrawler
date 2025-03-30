package com.akashgill3.githubcrawler.github.model;

import java.util.List;

public record PrincipleMetadata(
        String name,
        String owner,
        String value,
        List<String> tags) implements Metadata {
}
