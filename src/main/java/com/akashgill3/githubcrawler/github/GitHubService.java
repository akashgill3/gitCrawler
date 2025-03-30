package com.akashgill3.githubcrawler.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final GitHubProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Principle> principles;
    private final ExecutorService executorService;

    private GHRepository repo;

    public GitHubService(GitHubProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.principles = new ConcurrentHashMap<>();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void init() throws IOException {
        GitHub gitHub = new GitHubBuilder().withOAuthToken(properties.token()).build();
        repo = gitHub.getRepository(properties.repository());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup() {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting github indexing");
                Instant start = Instant.now();

                // Create and wait for the completion of the indexing operation
                indexPrinciples().join();

                Instant end = Instant.now();
                log.info("Indexed data in {} ms", Duration.between(start, end).toMillis());
            } catch (Exception e) {
                log.error("Failed to index data", e);
            }
        }, executorService);
    }

    /**
     * Indexes Principles, reindexing only those that have changed.
     * Returns a CompletableFuture that completes when all indexing is done.
     */
    private CompletableFuture<Void> indexPrinciples() {
        try {
            if (repo == null) init(); // Ensure repo is initialized
            List<GHContent> rootContent = repo.getDirectoryContent("/");

            List<CompletableFuture<Void>> tasks = new ArrayList<>();

            for (GHContent content : rootContent) {
                if (content.isDirectory()) {
                    CompletableFuture<Void> task = processPrinciple(repo, content.getPath())
                            .thenAccept(principle -> {
                                principles.put(content.getName(), principle);
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

    private CompletableFuture<Principle> processPrinciple(GHRepository repo, String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String directoryName = getLastPartOfPath(path).toLowerCase();

                // Get metadata from JSON file
                GHContent jsonContent = repo.getFileContent(path + "/" + directoryName + ".json");
                String jsonString = jsonContent.getContent();
                var metadata = parseMetadata(jsonString, PrincipleMetadata.class);

                // Get content from MD file
                GHContent mdContent = repo.getFileContent(path + "/" + directoryName + ".md");
                String markdownContent = mdContent.getContent();

                // Process sub-directories (sub-practices)
                List<GHContent> directoryContent = repo.getDirectoryContent(path);
                ConcurrentMap<String, Practise> practises = new ConcurrentHashMap<>();

                List<CompletableFuture<Void>> subTasks = new ArrayList<>();

                for (GHContent content : directoryContent) {
                    if (content.isDirectory()) {
                        CompletableFuture<Void> subTask = processPractise(repo, content.getPath())
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
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }

    private CompletableFuture<Practise> processPractise(GHRepository repo, String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String directoryName = getLastPartOfPath(path).toLowerCase();

                // Get metadata from JSON file
                var jsonContent = repo.getFileContent(path + "/" + directoryName + ".json");
                var jsonString = jsonContent.getContent();
                var metadata = parseMetadata(jsonString, PractiseMetadata.class);

                // Get content from MD file
                var mdContent = repo.getFileContent(path + "/" + directoryName + ".md");
                var markdownContent = mdContent.getContent();

                // Process sub-directories (sub-practices)
                List<GHContent> directoryContent = repo.getDirectoryContent(path);
                ConcurrentMap<String, Practise> subPractises = new ConcurrentHashMap<>();

                List<CompletableFuture<Void>> subTasks = new ArrayList<>();

                for (GHContent content : directoryContent) {
                    if (content.isDirectory()) {
                        CompletableFuture<Void> subTask = processPractise(repo, content.getPath())
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
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }

    private String getLastPartOfPath(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    private <T extends Metadata> T parseMetadata(String json, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(json, type);
    }

    sealed interface Node<T extends Metadata> permits Principle, Practise {
        String content();

        T metadata();
    }

    sealed interface Metadata permits PrincipleMetadata, PractiseMetadata {
        String name();

        String owner();

        List<String> tags();
    }

    record PrincipleMetadata(
            String name,
            String owner,
            String value,
            List<String> tags) implements Metadata {
    }

    record PractiseMetadata(
            String name,
            String owner,
            String metrics,
            List<String> tags) implements Metadata {
    }

    record Principle(
            String content,
            PrincipleMetadata metadata,
            Map<String, Practise> practises) implements Node<PrincipleMetadata> {
    }

    record Practise(
            String content,
            PractiseMetadata metadata,
            Map<String, Practise> subPractises) implements Node<PractiseMetadata> {
    }

    @RestController
    @RequestMapping("/api")
    static class RepositoryDataController {

        private final GitHubService gitHubService;

        public RepositoryDataController(GitHubService gitHubService) {
            this.gitHubService = gitHubService;
        }

        @GetMapping("/principles")
        public Map<String, Principle> getPrinciples() {
            return gitHubService.getPrinciples();
        }

        @GetMapping("/principles/{name}")
        public Principle getPrincipleByName(@PathVariable String name) {
            return gitHubService.getPrinciples().get(name);
        }
    }

    private Map<String, Principle> getPrinciples() {
        return principles;
    }
}
