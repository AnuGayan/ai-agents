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

### 3. Multi-Branch OSGI Dependencies Agent
Extracts OSGI feature dependencies from multiple repositories across multiple branches matching a specific pattern. This agent is designed to analyze dependencies across multiple support branches in one or more repositories.

**Key Features:**
- Processes multiple Git repositories in a single run
- Automatically discovers and filters branches that start with `support-` and end with `x-full`
- Extracts dependencies from each matching branch using the OSGI Feature Dependencies Agent
- Outputs results to a CSV file with columns: Repository, Branch, GroupId, ArtifactId, Version
- Handles errors gracefully - continues processing even if some repositories or branches fail

**Usage:**
```bash
java -jar ai-agents.jar extract-multi-branch <output-csv-file> <repo-url-1> [repo-url-2] ... [token]
```

**Parameters:**
- `output-csv-file`: Path where the CSV file will be created (e.g., `dependencies.csv`)
- `repo-url-1, repo-url-2, ...`: One or more Git repository URLs to process
- `token`: Optional GitHub token for private repositories (should be the last argument if provided)

**Example:**
```bash
# Process single repository
java -jar ai-agents.jar extract-multi-branch dependencies.csv https://github.com/wso2/carbon-apimgt

# Process multiple repositories
java -jar ai-agents.jar extract-multi-branch dependencies.csv \
  https://github.com/wso2/carbon-apimgt \
  https://github.com/wso2/carbon-identity

# With authentication token
java -jar ai-agents.jar extract-multi-branch dependencies.csv \
  https://github.com/user/private-repo \
  ghp_your_token_here
```

**Output Format:**
The generated CSV file contains the following columns:
- `Repository`: Name of the repository (extracted from URL)
- `Branch`: Branch name (e.g., `support-4.2.x-full`)
- `GroupId`: Maven dependency group ID
- `ArtifactId`: Maven dependency artifact ID
- `Version`: Dependency version

**Branch Filtering:**
Only branches matching the pattern `support-*x-full` are processed. For example:
- ✅ `support-4.2.x-full` - Processed
- ✅ `support-5.0.x-full` - Processed
- ❌ `support-4.1.x` - Skipped (doesn't end with `x-full`)
- ❌ `main` - Skipped (doesn't start with `support-`)
- ❌ `develop` - Skipped (doesn't match pattern)

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

### Multi-Branch OSGI Dependencies Agent
1. For each provided repository URL:
   - Lists all remote branches using Git ls-remote
   - Filters branches to find those matching the pattern `support-*x-full`
   - For each matching branch:
     - Extracts dependencies using the OSGI Feature Dependencies Agent
     - Records each dependency with repository name, branch name, and Maven coordinates
2. Aggregates all dependencies from all repositories and branches
3. Writes the results to a CSV file with proper escaping for special characters
4. Handles errors gracefully - if a repository or branch fails, it logs the error and continues
5. Returns the total count of dependency records extracted

**Performance Considerations:**
- Each branch is cloned separately, so processing time scales with the number of matching branches
- Failed operations don't stop the overall process - the agent continues with remaining repositories/branches
- Temporary directories are automatically cleaned up after each branch is processed

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.