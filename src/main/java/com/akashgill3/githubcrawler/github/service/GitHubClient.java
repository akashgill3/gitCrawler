package com.akashgill3.githubcrawler.github.service;

import com.akashgill3.githubcrawler.github.config.GitHubProperties;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class GitHubClient {
    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private final GitHubProperties properties;
    private GitHub gitHub;
    private GHRepository repository;

    public GitHubClient(GitHubProperties properties) {
        this.properties = properties;
    }

    public void init() throws IOException {
        if (gitHub == null) {
            gitHub = new GitHubBuilder().withOAuthToken(properties.token()).build();
            repository = gitHub.getRepository(properties.repository());
            log.info("GitHub client initialized for repository: {}", properties.repository());
        }
    }

    public GHRateLimit getRateLimit() throws IOException {
        ensureInitialized();
        return gitHub.getRateLimit();
    }

    /**
     * Get content from the root directory
     */
    public List<GHContent> getRootContent() {
        try {
            ensureInitialized();
            return repository.getDirectoryContent("/");
        } catch (IOException e) {
            throw new RuntimeException("Failed to get root content", e);
        }
    }

    /**
     * Get content of a specific directory
     */
    public List<GHContent> getDirectoryContent(String path) {
        try {
            ensureInitialized();
            return repository.getDirectoryContent(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get directory content: " + path, e);
        }
    }

    /**
     * Get content of a specific file
     */
    public String getFileContent(String path) {
        try {
            ensureInitialized();
            return repository.getFileContent(path).getContent();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get file content: " + path, e);
        }
    }

    private void ensureInitialized() throws IOException {
        if (gitHub == null || repository == null) {
            init();
        }
    }
}
