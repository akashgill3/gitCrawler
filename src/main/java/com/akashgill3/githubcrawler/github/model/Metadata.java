package com.akashgill3.githubcrawler.github.model;

import java.util.List;

public sealed interface Metadata permits PrincipleMetadata, PractiseMetadata {
    String name();

    String owner();

    List<String> tags();
}