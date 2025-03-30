package com.akashgill3.githubcrawler.github.model;

public sealed interface GitHubNode<T extends Metadata> permits Principle, Practise {
    String content();

    T metadata();
}
