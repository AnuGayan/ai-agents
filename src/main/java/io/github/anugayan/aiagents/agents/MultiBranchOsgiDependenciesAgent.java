package io.github.anugayan.aiagents.agents;

import io.github.anugayan.aiagents.utils.GitRepoCloner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent to extract OSGI feature dependencies from multiple repositories across multiple branches
 * Specifically targets branches starting with "support-" and ending with "x-full"
 */
public class MultiBranchOsgiDependenciesAgent {
    private static final Logger logger = LoggerFactory.getLogger(MultiBranchOsgiDependenciesAgent.class);
    
    private final OsgiFeatureDependenciesAgent featureAgent;
    
    public MultiBranchOsgiDependenciesAgent() {
        this.featureAgent = new OsgiFeatureDependenciesAgent();
    }
    
    /**
     * Extract dependencies from multiple repositories and their matching branches
     * 
     * @param repoUrls List of Git repository URLs
     * @param token GitHub token (can be null for public repos)
     * @param outputFilePath Path to the output CSV file
     * @return Number of dependencies extracted
     */
    public int extractDependencies(List<String> repoUrls, String token, String outputFilePath) {
        logger.info("Starting multi-branch OSGI dependency extraction for {} repositories", repoUrls.size());
        
        List<DependencyRecord> allRecords = new ArrayList<>();
        
        for (String repoUrl : repoUrls) {
            try {
                String repoName = extractRepoName(repoUrl);
                logger.info("Processing repository: {} ({})", repoName, repoUrl);
                
                // List all branches
                List<String> allBranches = GitRepoCloner.listBranches(repoUrl, token);
                logger.info("Found {} total branches in {}", allBranches.size(), repoName);
                
                // Filter branches that match the pattern: support-*x-full
                List<String> matchingBranches = filterBranches(allBranches);
                logger.info("Found {} matching branches (support-*x-full) in {}", matchingBranches.size(), repoName);
                
                // Process each matching branch
                for (String branch : matchingBranches) {
                    logger.info("Processing branch: {} in repository: {}", branch, repoName);
                    
                    try {
                        // Extract dependencies using OsgiFeatureDependenciesAgent
                        Map<String, List<OsgiFeatureDependenciesAgent.OsgiDependency>> dependencies = 
                            featureAgent.extractFeatureDependencies(repoUrl, branch, token);
                        
                        // Convert to records
                        for (Map.Entry<String, List<OsgiFeatureDependenciesAgent.OsgiDependency>> entry : dependencies.entrySet()) {
                            for (OsgiFeatureDependenciesAgent.OsgiDependency dep : entry.getValue()) {
                                DependencyRecord record = new DependencyRecord(
                                    repoName,
                                    branch,
                                    dep.getGroupId(),
                                    dep.getArtifactId(),
                                    dep.getVersion()
                                );
                                allRecords.add(record);
                            }
                        }
                        
                        logger.info("Extracted {} dependencies from branch {} in repository {}", 
                                  dependencies.values().stream().mapToInt(List::size).sum(), 
                                  branch, repoName);
                        
                    } catch (Exception e) {
                        logger.error("Error extracting dependencies from branch {} in repository {}", 
                                   branch, repoName, e);
                        // Continue with next branch
                    }
                }
                
            } catch (Exception e) {
                logger.error("Error processing repository: {}", repoUrl, e);
                // Continue with next repository
            }
        }
        
        // Write to CSV
        try {
            writeToCsv(allRecords, outputFilePath);
            logger.info("Successfully wrote {} dependency records to {}", allRecords.size(), outputFilePath);
        } catch (IOException e) {
            logger.error("Error writing CSV file: {}", outputFilePath, e);
        }
        
        return allRecords.size();
    }
    
    /**
     * Extract repository name from URL
     * Example: https://github.com/user/repo.git -> repo
     */
    private String extractRepoName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String lastPart = parts[parts.length - 1];
        // Remove .git suffix if present
        if (lastPart.endsWith(".git")) {
            lastPart = lastPart.substring(0, lastPart.length() - 4);
        }
        return lastPart;
    }
    
    /**
     * Filter branches that start with "support-" and end with "x-full"
     */
    private List<String> filterBranches(List<String> branches) {
        List<String> matchingBranches = new ArrayList<>();
        
        for (String branch : branches) {
            if (branch.startsWith("support-") && branch.endsWith("x-full")) {
                matchingBranches.add(branch);
            }
        }
        
        return matchingBranches;
    }
    
    /**
     * Write dependency records to CSV file
     */
    private void writeToCsv(List<DependencyRecord> records, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write header
            writer.write("Repository,Branch,GroupId,ArtifactId,Version");
            writer.newLine();
            
            // Write records
            for (DependencyRecord record : records) {
                writer.write(escapeCsv(record.repoName));
                writer.write(",");
                writer.write(escapeCsv(record.branch));
                writer.write(",");
                writer.write(escapeCsv(record.groupId));
                writer.write(",");
                writer.write(escapeCsv(record.artifactId));
                writer.write(",");
                writer.write(escapeCsv(record.version));
                writer.newLine();
            }
        }
    }
    
    /**
     * Escape CSV field (handle commas and quotes)
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
    
    /**
     * Internal class to represent a dependency record in the CSV
     */
    private static class DependencyRecord {
        final String repoName;
        final String branch;
        final String groupId;
        final String artifactId;
        final String version;
        
        DependencyRecord(String repoName, String branch, String groupId, String artifactId, String version) {
            this.repoName = repoName;
            this.branch = branch;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }
}
