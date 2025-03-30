package com.akashgill3.githubcrawler;

import com.akashgill3.githubcrawler.github.config.GitHubProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(GitHubProperties.class)
public class GithubCrawlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GithubCrawlerApplication.class, args);
	}

}


