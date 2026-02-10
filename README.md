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
Extracts all dependencies bundled in OSGI features from a Maven project.

**Supports multiple feature formats:**
- **Karaf-style feature XML files** (`*-features.xml`, `features.xml`)
- **Maven POM files** with carbon-p2-plugin configuration (extracts from `<bundles>` section)
- **p2.inf files** (Eclipse P2 format) - infrastructure ready

**What gets extracted:**
- For Maven POMs: Extracts bundles from the `carbon-p2-plugin` `<bundles>` section (the actual JARs that get packed)
- For Karaf XML: Extracts from `<bundle>` and `<feature>` elements
- Falls back to `<dependencies>` section if no carbon-p2-plugin found

**Version Resolution:**
- Property references like `${carbon.apimgt.version}` are resolved from parent POM properties
- Actual version values are extracted from the POM hierarchy
- Provides accurate, concrete version information

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
java -jar ai-agents.jar extract-features https://github.com/wso2/carbon-apimgt master
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
2. Scans for OSGI features in multiple formats:
   - **Karaf XML**: Files ending with `-features.xml` or named `features.xml`
   - **Maven POMs**: POMs in directories matching `*.feature` pattern
   - **p2.inf**: Eclipse P2 installation instructions (infrastructure ready)
3. Extracts dependencies from each format:
   - **Karaf XML**: Parses `<bundle>` and `<feature>` elements with Maven URL format
   - **Maven POMs**: Extracts bundles from `carbon-p2-plugin` `<bundles>` configuration (the actual JARs that get packed into the feature)
   - Falls back to `<dependencies>` section if no carbon-p2-plugin found
4. **Resolves version properties**: Property references like `${carbon.apimgt.version}` are resolved from parent POM properties
5. Organizes dependencies by feature name
6. Returns a complete list of all dependencies with their Maven coordinates and resolved versions
7. Cleans up the temporary directory

**Supported Repository Types:**
- Apache Karaf and Karaf-based projects (using feature XML files)
- WSO2 Carbon projects (using Maven POM features with carbon-p2-plugin)
- Eclipse P2-based projects (infrastructure ready)

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.