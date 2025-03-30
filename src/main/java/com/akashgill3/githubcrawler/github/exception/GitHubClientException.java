package com.akashgill3.githubcrawler.github.exception;

/**
 * Exception thrown when GitHub API operations fail
 */
public class GitHubClientException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public GitHubClientException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public GitHubClientException(String message, Throwable cause) {
        super(message, cause);
    }
} 