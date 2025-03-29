package com.akashgill3.githubcrawler.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public record GitHubProperties(String token, String repository) {
}
