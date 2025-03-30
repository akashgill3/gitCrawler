package com.akashgill3.githubcrawler.github.service;

import com.akashgill3.githubcrawler.github.config.GitHubProperties;
import com.akashgill3.githubcrawler.github.exception.GitHubClientException;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    /**
     * Initializes the GitHub client and repository.
     * Should be called at application startup.
     */
    public void init() {
        try {
            if (gitHub == null) {
                gitHub = new GitHubBuilder().withOAuthToken(properties.token()).build();
                repository = gitHub.getRepository(properties.repository());
                log.info("GitHub client initialized for repository: {}", properties.repository());
            }
        } catch (IOException e) {
            log.error("Failed to initialize GitHub client", e);
            throw new GitHubClientException("Failed to initialize GitHub client", e);
        }
    }

    /**
     * Gets the current rate limit status from GitHub.
     *
     * @return The current rate limit information
     * @throws GitHubClientException if the rate limit cannot be retrieved
     */
    public GHRateLimit getRateLimit() {
        try {
            ensureInitialized();
            return gitHub.getRateLimit();
        } catch (IOException e) {
            log.error("Failed to get rate limit", e);
            throw new GitHubClientException("Failed to get rate limit", e);
        }
    }

    /**
     * Get content from the root directory
     *
     * @return List of content items in the root directory
     * @throws GitHubClientException if the content cannot be retrieved
     */
    public List<GHContent> getRootContent() {
        try {
            ensureInitialized();
            return repository.getDirectoryContent("/");
        } catch (IOException e) {
            log.error("Failed to get root content", e);
            throw new GitHubClientException("Failed to get root content", e);
        }
    }

    /**
     * Get content of a specific directory
     *
     * @param path The directory path to get content from
     * @return List of content items in the specified directory
     * @throws GitHubClientException if the content cannot be retrieved
     */
    public List<GHContent> getDirectoryContent(String path) {
        try {
            ensureInitialized();
            return repository.getDirectoryContent(path);
        } catch (IOException e) {
            log.error("Failed to get directory content: {}", path, e);
            throw new GitHubClientException("Failed to get directory content: " + path, e);
        }
    }

    /**
     * Get content of a specific file
     *
     * @param path The file path to get content from
     * @return The content of the file as a String
     * @throws GitHubClientException if the file content cannot be retrieved
     */
    public String getFileContent(String path) {
        try {
            ensureInitialized();
            try (InputStream is = repository.getFileContent(path).read()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to get file content: {}", path, e);
            throw new GitHubClientException("Failed to get file content: " + path, e);
        }
    }

    /**
     * Ensures the GitHub client and repository are initialized.
     * This method is called internally before making any GitHub API calls.
     */
    private void ensureInitialized() {
        if (gitHub == null || repository == null) {
            init();
        }
    }
}
