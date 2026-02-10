package io.github.anugayan.aiagents.agents;

import io.github.anugayan.aiagents.utils.GitRepoCloner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MultiBranchOsgiDependenciesAgent
 */
class MultiBranchOsgiDependenciesAgentTest {
    
    // Test repository with OSGI features that has support branches
    private static final String KARAF_REPO_URL = "https://github.com/apache/karaf";
    
    private MultiBranchOsgiDependenciesAgent agent;
    private Path tempOutputFile;
    
    @BeforeEach
    void setUp() throws IOException {
        agent = new MultiBranchOsgiDependenciesAgent();
        tempOutputFile = Files.createTempFile("osgi-deps-", ".csv");
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (tempOutputFile != null && Files.exists(tempOutputFile)) {
            Files.delete(tempOutputFile);
        }
    }
    
    @Test
    @DisplayName("Test branch filtering - support-*x-full pattern")
    void testBranchFiltering() throws Exception {
        // Create a list of test branches
        List<String> testBranches = Arrays.asList(
            "main",
            "support-4.2.x-full",
            "support-4.3.x-full",
            "support-4.1.x",
            "support-5.0.x-full",
            "develop",
            "feature-branch",
            "support-3.0.x-full"
        );
        
        // Use reflection to access the private filterBranches method
        java.lang.reflect.Method method = MultiBranchOsgiDependenciesAgent.class.getDeclaredMethod(
            "filterBranches", List.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> filtered = (List<String>) method.invoke(agent, testBranches);
        
        // Assert
        assertEquals(4, filtered.size(), "Should filter to 4 matching branches");
        assertTrue(filtered.contains("support-4.2.x-full"));
        assertTrue(filtered.contains("support-4.3.x-full"));
        assertTrue(filtered.contains("support-5.0.x-full"));
        assertTrue(filtered.contains("support-3.0.x-full"));
        assertFalse(filtered.contains("main"));
        assertFalse(filtered.contains("support-4.1.x"));
    }
    
    @Test
    @DisplayName("Test repository name extraction")
    void testRepoNameExtraction() throws Exception {
        // Use reflection to access the private extractRepoName method
        java.lang.reflect.Method method = MultiBranchOsgiDependenciesAgent.class.getDeclaredMethod(
            "extractRepoName", String.class);
        method.setAccessible(true);
        
        // Test various URL formats
        assertEquals("karaf", method.invoke(agent, "https://github.com/apache/karaf"));
        assertEquals("karaf", method.invoke(agent, "https://github.com/apache/karaf.git"));
        assertEquals("carbon-apimgt", method.invoke(agent, "https://github.com/wso2/carbon-apimgt.git"));
        
        // Test with trailing slash
        assertEquals("karaf", method.invoke(agent, "https://github.com/apache/karaf/"));
        assertEquals("karaf", method.invoke(agent, "https://github.com/apache/karaf.git/"));
        assertEquals("karaf", method.invoke(agent, "https://github.com/apache/karaf//"));
    }
    
    @Test
    @DisplayName("Test CSV escaping")
    void testCsvEscaping() throws Exception {
        // Use reflection to access the private escapeCsv method
        java.lang.reflect.Method method = MultiBranchOsgiDependenciesAgent.class.getDeclaredMethod(
            "escapeCsv", String.class);
        method.setAccessible(true);
        
        // Test normal value
        assertEquals("test", method.invoke(agent, "test"));
        
        // Test value with comma
        assertEquals("\"test,value\"", method.invoke(agent, "test,value"));
        
        // Test value with quote
        assertEquals("\"test\"\"value\"", method.invoke(agent, "test\"value"));
        
        // Test value with newline
        assertEquals("\"test\nvalue\"", method.invoke(agent, "test\nvalue"));
        
        // Test null value
        assertEquals("", method.invoke(agent, (String) null));
    }
    
    @Test
    @DisplayName("Test CSV file creation and format")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void testCsvFileCreation() {
        // Use a simple repository with predictable content
        List<String> repoUrls = Arrays.asList(KARAF_REPO_URL);
        
        // Extract dependencies
        int recordCount = agent.extractDependencies(repoUrls, null, tempOutputFile.toString());
        
        // Verify CSV file was created
        assertTrue(Files.exists(tempOutputFile), "CSV file should be created");
        
        // Read and verify CSV content
        try (BufferedReader reader = new BufferedReader(new FileReader(tempOutputFile.toFile()))) {
            String header = reader.readLine();
            assertNotNull(header, "CSV should have a header");
            assertEquals("Repository,Branch,GroupId,ArtifactId,Version", header, "CSV header should match expected format");
            
            // Check that we have at least the header
            assertTrue(recordCount >= 0, "Record count should be non-negative");
            
            // If we have records, verify format
            if (recordCount > 0) {
                String firstLine = reader.readLine();
                assertNotNull(firstLine, "Should have at least one data line");
                
                String[] fields = firstLine.split(",");
                // Note: fields might be more than 5 if any field contains escaped commas
                assertTrue(fields.length >= 5, "Each line should have at least 5 fields");
            }
            
        } catch (IOException e) {
            fail("Error reading CSV file: " + e.getMessage());
        }
        
        System.out.println("Test completed successfully with " + recordCount + " records");
    }
    
    @Test
    @DisplayName("Test with empty repository list")
    void testEmptyRepositoryList() {
        List<String> repoUrls = Arrays.asList();
        
        int recordCount = agent.extractDependencies(repoUrls, null, tempOutputFile.toString());
        
        assertEquals(0, recordCount, "Should return 0 records for empty repository list");
        assertTrue(Files.exists(tempOutputFile), "CSV file should still be created");
    }
    
    @Test
    @DisplayName("Test with invalid repository URL")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testInvalidRepositoryUrl() {
        List<String> repoUrls = Arrays.asList("https://github.com/invalid/nonexistent-repo-xyz");
        
        // Should handle gracefully and not throw exception
        int recordCount = agent.extractDependencies(repoUrls, null, tempOutputFile.toString());
        
        assertEquals(0, recordCount, "Should return 0 records for invalid repository");
        assertTrue(Files.exists(tempOutputFile), "CSV file should still be created");
    }
    
    @Test
    @DisplayName("Test listing branches from repository")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testListBranches() throws Exception {
        // Test the GitRepoCloner.listBranches method
        List<String> branches = GitRepoCloner.listBranches(KARAF_REPO_URL, null);
        
        assertNotNull(branches, "Branch list should not be null");
        assertFalse(branches.isEmpty(), "Should find at least one branch");
        
        System.out.println("Found " + branches.size() + " branches in " + KARAF_REPO_URL);
        
        // Apache Karaf should have main branch
        assertTrue(branches.contains("main") || branches.contains("master"), 
                  "Should contain main or master branch");
    }
    
    @Test
    @DisplayName("Test single repository processing")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void testSingleRepositoryProcessing() {
        // Use a single repository
        List<String> repoUrls = Arrays.asList(
            "https://github.com/apache/karaf"
        );
        
        int recordCount = agent.extractDependencies(repoUrls, null, tempOutputFile.toString());
        
        assertTrue(recordCount >= 0, "Should process repository successfully");
        assertTrue(Files.exists(tempOutputFile), "CSV file should be created");
        
        System.out.println("Processed repository with " + recordCount + " total records");
    }
}
