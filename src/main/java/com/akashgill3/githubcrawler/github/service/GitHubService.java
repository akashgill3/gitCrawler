package com.akashgill3.githubcrawler.github.service;

import com.akashgill3.githubcrawler.github.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.GHContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final GitHubClient githubClient;
    private final ObjectMapper objectMapper;
    private final PrincipleCache principleCache;
    private final ExecutorService executorService;

    public GitHubService(GitHubClient gitHubClient, ObjectMapper objectMapper, PrincipleCache principleCache, ExecutorService executorService) {
        this.githubClient = gitHubClient;
        this.objectMapper = objectMapper;
        this.principleCache = principleCache;
        this.executorService = executorService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup() {
        CompletableFuture.runAsync(() -> {
            try {
                githubClient.init();

                log.info("Starting github indexing");
                log.info("Start of Indexing, remaining rate limit: {}", githubClient.getRateLimit().getRemaining());
                Instant start = Instant.now();

                // Create and wait for the completion of the indexing operation
                indexPrinciples().join();

                Instant end = Instant.now();
                log.info("Indexed data in {} ms", Duration.between(start, end).toMillis());
                log.info("End of Indexing, remaining rate limit: {}", githubClient.getRateLimit().getRemaining());
            } catch (IOException e) {
                log.error("Failed to index data", e);
            }
        }, executorService);
    }

    /**
     * Indexes All Principles.
     * Returns a CompletableFuture that completes when all indexing is done.
     */
    private CompletableFuture<Void> indexPrinciples() {
        try {
            githubClient.init();
            List<GHContent> rootContent = githubClient.getRootContent();

            List<CompletableFuture<Void>> tasks = new ArrayList<>();

            for (GHContent content : rootContent) {
                if (content.isDirectory()) {
                    CompletableFuture<Void> task = processPrinciple(content.getPath())
                            .thenAccept(principle -> {
                                principleCache.put(content.getName(), principle);
                                log.info("Principle: {} indexed", content.getName());
                            })
                            .exceptionally(e -> {
                                log.error("Failed to index Principle: {}", content.getName(), e);
                                return null;
                            });
                    tasks.add(task);
                }
            }

            // Return a future that completes when all tasks are done
            return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
        } catch (IOException e) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }


    public void reindexPrinciples(Set<String> affectedPrinciples) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting selective reindexing of {} principles", affectedPrinciples.size());
                Instant start = Instant.now();
                // Ensure repo is initialized
                githubClient.init();
                // Process each affected principle
                List<CompletableFuture<Void>> tasks = new ArrayList<>();

                for (String principleName : affectedPrinciples) {
                    log.info("Reindexing principle: {}", principleName);

                    // Process this principle
                    CompletableFuture<Principle> principleFuture = processPrinciple(principleName);

                    CompletableFuture<Void> task = principleFuture.thenAccept(principle -> {
                        principleCache.put(principleName, principle);
                        log.info("Principle: {} reindexed successfully", principleName);
                    }).exceptionally(ex -> {
                        log.error("Failed to reindex principle: {}", principleName, ex);
                        return null;
                    });

                    tasks.add(task);
                }

                // Wait for all reindexing tasks to complete
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

                Instant end = Instant.now();
                log.info("Reindexed {} principles in {} ms", affectedPrinciples.size(), Duration.between(start, end).toMillis());

            } catch (Exception e) {
                log.error("Failed to reindex principles", e);
            }
        }, executorService);
    }

    private CompletableFuture<Principle> processPrinciple(String path) {
        return CompletableFuture.supplyAsync(() -> {
            String directoryName = getLastPartOfPath(path);

            String jsonPath = path + "/" + directoryName + ".json";
            String mdPath = path + "/" + directoryName + ".md";

            // Get metadata from JSON file
            var jsonString = githubClient.getFileContent(jsonPath);
            var metadata = parseMetadata(jsonString, PrincipleMetadata.class);

            // Get content from MD file
            String markdownContent = githubClient.getFileContent(mdPath);

            // Process sub-directories (sub-practices)
            List<GHContent> directoryContent = githubClient.getDirectoryContent(path);

            ConcurrentMap<String, Practise> practises = new ConcurrentHashMap<>();

            List<CompletableFuture<Void>> subTasks = new ArrayList<>();

            for (GHContent content : directoryContent) {
                if (content.isDirectory()) {
                    CompletableFuture<Void> subTask = processPractise(content.getPath())
                            .thenAccept(practise -> {
                                practises.put(content.getName(), practise);
                                log.info("Practise: {} indexed", content.getName());
                            })
                            .exceptionally(e -> {
                                log.error("Failed to process practice: {}", content.getName(), e);
                                return null;
                            });
                    subTasks.add(subTask);
                }
            }

            // Wait for all sub-practices to be processed
            CompletableFuture.allOf(subTasks.toArray(new CompletableFuture[0])).join();

            return new Principle(markdownContent, metadata, practises);
        }, executorService);
    }

    private CompletableFuture<Practise> processPractise(String path) {
        return CompletableFuture.supplyAsync(() -> {
            String directoryName = getLastPartOfPath(path);

            String jsonPath = path + "/" + directoryName + ".json";
            String mdPath = path + "/" + directoryName + ".md";

            // Get metadata from JSON file
            var jsonString = githubClient.getFileContent(jsonPath);
            var metadata = parseMetadata(jsonString, PractiseMetadata.class);

            // Get content from MD file
            var markdownContent = githubClient.getFileContent(mdPath);

            // Process sub-directories (sub-practices)
            List<GHContent> directoryContent = githubClient.getDirectoryContent(path);
            ConcurrentMap<String, Practise> subPractises = new ConcurrentHashMap<>();

            List<CompletableFuture<Void>> subTasks = new ArrayList<>();

            for (GHContent content : directoryContent) {
                if (content.isDirectory()) {
                    CompletableFuture<Void> subTask = processPractise(content.getPath())
                            .thenAccept(practise -> {
                                subPractises.put(content.getName(), practise);
                                log.info("Practise: {} indexed", content.getName());
                            })
                            .exceptionally(e -> {
                                log.error("Failed to process sub-practice: {}", content.getName(), e);
                                return null;
                            });
                    subTasks.add(subTask);
                }
            }

            // Wait for all sub-practices to be processed
            CompletableFuture.allOf(subTasks.toArray(new CompletableFuture[0])).join();

            return new Practise(markdownContent, metadata, subPractises);
        }, executorService);
    }

    private String getLastPartOfPath(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1].toLowerCase();
    }

    private <T extends Metadata> T parseMetadata(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse metadata: {}", json, e);
        }
        return null;
    }
}
