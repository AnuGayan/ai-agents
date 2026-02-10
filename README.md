# AI Agents for Maven Repository Analysis

This repository contains AI agents for analyzing Maven repositories and extracting information from Java projects.

## Features

The following agents are currently available:

### 1. Maven Dependency Version Agent
Finds the version of a specific dependency in a Maven project from a GitHub repository.

**Usage:**
```bash
java -jar ai-agents.jar find-version <repo-url> <branch> <groupId> <artifactId> [token]
```

**Parameters:**
- `repo-url`: Git repository URL (e.g., `https://github.com/user/repo`)
- `branch`: Branch name to analyze
- `groupId`: Maven dependency groupId
- `artifactId`: Maven dependency artifactId
- `token`: Optional GitHub token for private repositories

**Example:**
```bash
java -jar ai-agents.jar find-version https://github.com/apache/maven master org.slf4j slf4j-api
```

### 2. OSGI Feature Dependencies Agent
Extracts all dependencies bundled in OSGI feature files from a Maven project.

**Usage:**
```bash
java -jar ai-agents.jar extract-features <repo-url> <branch> [token]
```

**Parameters:**
- `repo-url`: Git repository URL
- `branch`: Branch name to analyze
- `token`: Optional GitHub token for private repositories

**Example:**
```bash
java -jar ai-agents.jar extract-features https://github.com/apache/karaf main
```

## Building

Build the project using Maven:

```bash
mvn clean package
```

This will create a JAR file in the `target` directory: `ai-agents-1.0.0-SNAPSHOT.jar`

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## How It Works

### Maven Dependency Version Agent
1. Clones the specified Git repository to a temporary directory
2. Recursively scans for all `pom.xml` files in the repository
3. Parses each POM file to find the specified dependency
4. Returns the version from either the `<dependencies>` or `<dependencyManagement>` section
5. Cleans up the temporary directory

### OSGI Feature Dependencies Agent
1. Clones the specified Git repository to a temporary directory
2. Recursively scans for OSGI feature XML files (files ending with `-features.xml` or named `features.xml`)
3. Parses each feature file to extract bundle and feature dependencies
4. Organizes dependencies by feature name
5. Returns a complete list of all dependencies with their Maven coordinates
6. Cleans up the temporary directory

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.