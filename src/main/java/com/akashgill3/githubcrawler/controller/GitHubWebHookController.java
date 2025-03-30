package com.akashgill3.githubcrawler.controller;

import com.akashgill3.githubcrawler.github.GitHubService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/")
@CrossOrigin(maxAge = 3600)
public class GitHubWebHookController {
    private static final Logger log = LoggerFactory.getLogger(GitHubWebHookController.class);
    private final ObjectMapper jacksonObjectMapper;
    private final GitHubService gitHubService;

    public GitHubWebHookController(ObjectMapper jacksonObjectMapper, GitHubService gitHubService) {
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.gitHubService = gitHubService;
    }

    @PostMapping("/github")
    public void handleGitHubWebHook(@RequestBody JsonNode payload) {
        String ref = payload.path("ref").asText();
        log.info("Push event for ref {}", ref);

        // Check if it's a push to the main branch
        if (ref.equals("refs/heads/main")) {
            // Extract modified file paths from the payload
            Set<String> affectedPrinciples = extractAffectedPrinciples(payload);

            if (!affectedPrinciples.isEmpty()) {
                log.info("Affected principles to reindex: {}", affectedPrinciples);
                gitHubService.reindexPrinciples(affectedPrinciples);
            } else {
                log.info("No principles affected by this push");
            }
        } else {
            log.info("Push to non-main branch, ignoring");
        }
    }

    private Set<String> extractAffectedPrinciples(JsonNode payload) {
        Set<String> principles = new HashSet<>();

        // Get the head commit information
        JsonNode headCommit = payload.path("head_commit");

        // Extract changed file paths from added, removed, and modified lists
        addFilePaths(principles, headCommit.path("added"));
        addFilePaths(principles, headCommit.path("removed"));
        addFilePaths(principles, headCommit.path("modified"));

        return principles;
    }

    /**
     * Extracts principle names from file paths and adds them to the set
     */
    private void addFilePaths(Set<String> principles, JsonNode pathsNode) {
        if (pathsNode.isArray()) {
            for (JsonNode pathNode : pathsNode) {
                String path = pathNode.asText();
                String principleName = extractPrincipleName(path);
                if (principleName != null) {
                    principles.add(principleName);
                }
            }
        }
    }

    /**
     * Extracts the top-level principle name from a file path
     */
    private String extractPrincipleName(String path) {
        if (path != null && !path.isEmpty()) {
            String[] parts = path.split("/");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return null;
    }
}
