package com.akashgill3.githubcrawler.github;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final GitHubProperties properties;
    private final ObjectMapper objectMapper;
    private ConcurrentMap<String, Principle> principles;
    private GHRepository repo;

    public GitHubService(GitHubProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void init() throws IOException {
        GitHub gitHub = new GitHubBuilder().withOAuthToken(properties.token()).build();
        repo = gitHub.getRepository(properties.repository());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup() {
        Thread.ofVirtual().start(() -> {
            try {
                log.info("Starting github indexing");
                Instant start = Instant.now();
                indexPrinciples();
                Instant end = Instant.now();
                log.info("Indexed data in {} ms", Duration.between(start, end).toMillis());
            } catch (IOException e) {
                log.error("Failed to index data", e);
            }
        });
    }

    /**
     * Indexes Principles, reindexing only those that have changed.
     */
    private void indexPrinciples() throws IOException {
        if (repo == null) init(); // Ensure repo is initialized
        List<GHContent> rootContent = repo.getDirectoryContent("/");

        principles = new ConcurrentHashMap<>();

        for (GHContent content : rootContent) {
            if (content.isDirectory()) {
                Thread.ofVirtual().start(() -> {
                    try {
                        principles.put(content.getName(), processPrinciple(repo, content.getPath()));
                        log.info("Principle: {} indexed", content.getName());
                    } catch (IOException e) {
                        log.info("Failed to index Principle: {}", content.getName());
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private Principle processPrinciple(GHRepository repo, String path) throws IOException {
        String directoryName = getLastPartOfPath(path).toLowerCase();
        // Get metadata from JSON file
        GHContent jsonContent = repo.getFileContent(path + "/" + directoryName + ".json");
        String jsonString = jsonContent.getContent();
        var metadata = (PrincipleMetadata) parseMetadata(jsonString, true);

        // Get content from MD file
        GHContent mdContent = repo.getFileContent(path + "/" + directoryName + ".md");
        String markdownContent = mdContent.getContent();

        // Process sub-directories (practices)
        List<Practise> practises = new ArrayList<>();
        List<GHContent> directoryContent = repo.getDirectoryContent(path);

        for (GHContent content : directoryContent) {
            if (content.isDirectory()) {
                practises.add(processPractise(repo, content.getPath()));
            }
        }

        return new Principle(
                markdownContent,
                practises,
                metadata
        );
    }


    private Practise processPractise(GHRepository repository, String path) throws IOException {
        String directoryName = getLastPartOfPath(path).toLowerCase();

        // Get metadata from JSON file
        var jsonContent = repository.getFileContent(path + "/" + directoryName + ".json");
        var jsonString = jsonContent.getContent();
        var metadata = (PractiseMetadata) parseMetadata(jsonString, false);

        // Get content from MD file
        var mdContent = repository.getFileContent(path + "/" + directoryName + ".md");
        var markdownContent = mdContent.getContent();

        // Process sub-directories (sub-practices)
        List<Practise> subPractises = new ArrayList<>();
        List<GHContent> directoryContent = repository.getDirectoryContent(path);

        for (GHContent content : directoryContent) {
            if (content.isDirectory()) {
                subPractises.add(processPractise(repository, content.getPath()));
            }
        }

        return new Practise(
                markdownContent,
                subPractises,
                metadata
        );
    }

    private String getLastPartOfPath(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    private Metadata parseMetadata(String json, boolean isPrinciple) throws JsonProcessingException {
        if (isPrinciple) {
            PrincipleMetadata principleMetadata = objectMapper.readValue(json, PrincipleMetadata.class);
            log.info(principleMetadata.toString());
            return principleMetadata;
        } else {
            PractiseMetadata practiseMetadata = objectMapper.readValue(json, PractiseMetadata.class);
            log.info(practiseMetadata.toString());
            return practiseMetadata;
        }
    }


    sealed interface Node permits Principle, Practise {
        String content();
    }

    sealed interface Metadata permits PrincipleMetadata, PractiseMetadata{
        String name();
        String owner();
        List<String> tags();
    }
    record PrincipleMetadata(
           String name,
           String owner,
           String value,
           List<String> tags
    ) implements Metadata{}

    record PractiseMetadata(
            String name,
            String owner,
            String metrics,
            List<String> tags
    ) implements Metadata{}

    record Principle(
            String content,
            List<Practise> practises,
            PrincipleMetadata metadata
    ) implements Node {}

    record Practise(
            String content,
            List<Practise> subPractises,
            PractiseMetadata metadata
    ) implements Node {}

    @RestController
    @RequestMapping("/")
    static class RepositoryDataController{
        private final GitHubService gitHubService;
        public RepositoryDataController(GitHubService gitHubService) {
            this.gitHubService = gitHubService;
        }

        @GetMapping("/principles")
        public Map<String, Principle> getPrinciples() throws IOException {
            return gitHubService.getPrinciples();
        }
    }

    private Map<String, Principle> getPrinciples() {
        return principles;
    }
}
