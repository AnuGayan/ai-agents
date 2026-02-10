package io.github.anugayan.aiagents;

import io.github.anugayan.aiagents.agents.MavenDependencyVersionAgent;
import io.github.anugayan.aiagents.agents.MultiBranchOsgiDependenciesAgent;
import io.github.anugayan.aiagents.agents.OsgiFeatureDependenciesAgent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Main class to demonstrate AI agents
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("AI Agents for Maven Repository Analysis");
        System.out.println("=========================================\n");

        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];

        switch (command) {
            case "find-version":
                handleFindVersion(args);
                break;
            case "extract-features":
                handleExtractFeatures(args);
                break;
            case "extract-multi-branch":
                handleExtractMultiBranch(args);
                break;
            default:
                System.err.println("Unknown command: " + command);
                printUsage();
        }
    }

    private static void handleFindVersion(String[] args) {
        if (args.length < 5) {
            System.err.println("Usage: find-version <repo-url> <branch> <groupId> <artifactId> [token]");
            return;
        }

        String repoUrl = args[1];
        String branch = args[2];
        String groupId = args[3];
        String artifactId = args[4];
        String token = args.length > 5 ? args[5] : null;

        System.out.println("Finding dependency version...");
        System.out.println("Repository: " + repoUrl);
        System.out.println("Branch: " + branch);
        System.out.println("Dependency: " + groupId + ":" + artifactId);
        System.out.println();

        MavenDependencyVersionAgent agent = new MavenDependencyVersionAgent();
        String version = agent.findDependencyVersion(repoUrl, branch, token, groupId, artifactId);

        if (version != null) {
            System.out.println("SUCCESS: Found version: " + version);
        } else {
            System.out.println("NOT FOUND: Dependency not found in repository");
        }
    }

    private static void handleExtractFeatures(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: extract-features <repo-url> <branch> [token]");
            return;
        }

        String repoUrl = args[1];
        String branch = args[2];
        String token = args.length > 3 ? args[3] : null;

        System.out.println("Extracting OSGI feature dependencies...");
        System.out.println("Repository: " + repoUrl);
        System.out.println("Branch: " + branch);
        System.out.println();

        OsgiFeatureDependenciesAgent agent = new OsgiFeatureDependenciesAgent();
        Map<String, List<OsgiFeatureDependenciesAgent.OsgiDependency>> dependencies = 
            agent.extractFeatureDependencies(repoUrl, branch, token);

        if (dependencies.isEmpty()) {
            System.out.println("No OSGI features found in repository");
        } else {
            System.out.println("Found " + dependencies.size() + " feature(s):\n");
            
            for (Map.Entry<String, List<OsgiFeatureDependenciesAgent.OsgiDependency>> entry : dependencies.entrySet()) {
                System.out.println("Feature: " + entry.getKey());
                System.out.println("Dependencies:");
                for (OsgiFeatureDependenciesAgent.OsgiDependency dep : entry.getValue()) {
                    System.out.println("  - " + dep);
                }
                System.out.println();
            }
        }
    }

    private static void handleExtractMultiBranch(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: extract-multi-branch <output-csv-file> <repo-url-1> [repo-url-2] ... [token]");
            System.err.println("Note: Token should be the last argument if provided");
            return;
        }

        String outputFile = args[1];
        
        // Parse repository URLs and optional token
        List<String> repoUrls = new ArrayList<>();
        String token = null;
        
        // Determine if last argument is a token (starts with gh or has specific patterns)
        // Also ensure it's not a URL (doesn't contain :// or /)
        String lastArg = args[args.length - 1];
        boolean hasToken = (lastArg.startsWith("ghp_") || lastArg.startsWith("github_pat_")) 
                          && !lastArg.contains("://") && !lastArg.contains("/");
        
        int endIndex = hasToken ? args.length - 1 : args.length;
        
        for (int i = 2; i < endIndex; i++) {
            repoUrls.add(args[i]);
        }
        
        if (hasToken) {
            token = lastArg;
        }

        System.out.println("Extracting OSGI feature dependencies from multiple repositories and branches...");
        System.out.println("Output file: " + outputFile);
        System.out.println("Repositories: " + repoUrls.size());
        for (String repoUrl : repoUrls) {
            System.out.println("  - " + repoUrl);
        }
        System.out.println("Branch filter: support-*x-full");
        System.out.println();

        MultiBranchOsgiDependenciesAgent agent = new MultiBranchOsgiDependenciesAgent();
        int recordCount = agent.extractDependencies(repoUrls, token, outputFile);

        System.out.println("\nSUCCESS: Extracted " + recordCount + " dependency records");
        System.out.println("Output written to: " + outputFile);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar ai-agents.jar find-version <repo-url> <branch> <groupId> <artifactId> [token]");
        System.out.println("  java -jar ai-agents.jar extract-features <repo-url> <branch> [token]");
        System.out.println("  java -jar ai-agents.jar extract-multi-branch <output-csv-file> <repo-url-1> [repo-url-2] ... [token]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  find-version         - Find the version of a dependency in a Maven project");
        System.out.println("  extract-features     - Extract dependencies from OSGI feature files");
        System.out.println("  extract-multi-branch - Extract dependencies from multiple repos across support-*x-full branches to CSV");
        System.out.println();
        System.out.println("Parameters:");
        System.out.println("  repo-url         - Git repository URL (e.g., https://github.com/user/repo)");
        System.out.println("  branch           - Branch name to analyze");
        System.out.println("  groupId          - Maven dependency groupId");
        System.out.println("  artifactId       - Maven dependency artifactId");
        System.out.println("  output-csv-file  - Path to output CSV file");
        System.out.println("  token            - Optional GitHub token for private repositories");
    }
}
