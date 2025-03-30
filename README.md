# GitHub Crawler

A Spring Boot application that indexes and serves content from a GitHub repository, providing a structured API for accessing principles and practices defined in the repository.

## Overview

GitHub Crawler connects to a specified GitHub repository, indexes its content according to a predefined structure, and provides RESTful API endpoints to access this content. It also listens for GitHub webhook events to selectively update content when changes are detected.

The application maintains an in-memory cache of the repository content, making access fast and efficient. It uses virtual threads and asynchronous processing to optimize indexing performance.

## Features

- **GitHub Repository Indexing**: Automatically indexes repository content at startup
- **Webhook Integration**: Listens for GitHub webhooks to update content when changes occur
- **In-Memory Caching**: Maintains content in memory for fast access
- **Hierarchical Content Structure**: Supports principles with nested practices
- **Asynchronous Processing**: Uses Java's virtual threads for efficient concurrent operations
- **REST API**: Provides endpoints for accessing indexed content

## Architecture

The application is structured with the following components:

### Controller Components

- **PrincipleController**: Exposes endpoints for accessing principles
- **WebhookController**: Handles incoming GitHub webhook events

### Service Components

- **GitHubClient**: Manages all interactions with the GitHub API
- **GitHubService**: Coordinates the indexing and processing of repository content
- **PrincipleCache**: Provides an in-memory cache of indexed principles

### Configuration

- **GitHubProperties**: Configures GitHub connection parameters
- **ThreadConfig**: Provides executor services for concurrent operations

### Data Model

- **Principle**: Represents a principle with its content, metadata, and associated practices
- **Practice**: Represents a practice with content, metadata, and optional sub-practices
- **Metadata**: Common interface for principle and practice metadata

## Getting Started

### Prerequisites

- Java 21 or later
- Maven
- GitHub personal access token with repository access

### Configuration

Create an `application.yml` file with your GitHub configuration:

```yaml
github:
  token: your-github-token
  repository: username/repository-name
```

### Building and Running

```bash
# Build the application
mvn clean package

# Run the application
java -jar target/github-crawler-0.0.1-SNAPSHOT.jar
```

### Setting Up GitHub Webhooks

1. Go to your GitHub repository settings
2. Navigate to Webhooks
3. Add a new webhook with the URL: `http://your-server/webhooks/github`
4. Set content type to `application/json`
5. Select the "Push" event

## API Endpoints

### Get All Principles

```
GET /api/principles
```

Returns a map of all indexed principles.

### Get a Specific Principle

```
GET /api/principles/{name}
```

Returns a specific principle by name.

## Repository Structure

The application expects your GitHub repository to be structured as follows:

```
repository-root/
├── principle-1/
│   ├── principle-1.md
│   ├── principle-1.json
│   ├── practice-1/
│   │   ├── practice-1.md
│   │   └── practice-1.json
│   └── practice-2/
│       ├── practice-2.md
│       └── practice-2.json
└── principle-2/
    ├── principle-2.md
    ├── principle-2.json
    └── ...
```

### Metadata Format

Each principle and practice should have an associated JSON file with metadata:

#### Principle JSON Example

```json
{
  "name": "Principle Name",
  "owner": "Owner Name",
  "value": "The value this principle provides",
  "tags": ["tag1", "tag2"]
}
```

#### Practice JSON Example

```json
{
  "name": "Practice Name",
  "owner": "Owner Name",
  "metrics": "How to measure this practice",
  "tags": ["tag1", "tag2"]
}
```

## Development

### Project Structure

```
com.akashgill3.githubcrawler
├── github
│   ├── controller                        # API endpoints
│   │   ├── PrincipleController
│   │   └── WebhookController
│   ├── service                       # Core logic
│   │   ├── GitHubClient
│   │   ├── GitHubService
│   │   └── PrincipleCache
│   ├── config                     # Configuration
│   │   ├── GitHubProperties
│   │   └── ThreadConfig
│   └── model                      # Data structures
│       ├── Metadata
│       ├── Node
│       ├── Principle
│       ├── PrincipleMetadata
│       ├── Practise
│       └── PractiseMetadata
```

## Performance Considerations

- The application uses Java's virtual threads for efficient concurrent operations
- Content is kept in memory for fast access
- GitHub API rate limits are tracked and logged
- Webhook handlers only update affected principles, not the entire repository
