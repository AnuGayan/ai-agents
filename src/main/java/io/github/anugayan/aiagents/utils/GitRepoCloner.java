package io.github.anugayan.aiagents.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for cloning Git repositories
 */
public class GitRepoCloner {
    private static final Logger logger = LoggerFactory.getLogger(GitRepoCloner.class);

    /**
     * Clone a Git repository to a temporary directory
     *
     * @param repoUrl Git repository URL
     * @param branch Branch name to checkout
     * @param token GitHub token for authentication (can be null for public repos)
     * @return File object pointing to the cloned repository
     * @throws GitAPIException if Git operation fails
     * @throws IOException if I/O operation fails
     */
    public static File cloneRepository(String repoUrl, String branch, String token) throws GitAPIException, IOException {
        Path tempDir = Files.createTempDirectory("git-repo-");
        logger.info("Cloning repository {} (branch: {}) to {}", repoUrl, branch, tempDir);

        Git git;
        if (token != null && !token.isEmpty()) {
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile())
                    .setBranch(branch)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                    .call();
        } else {
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile())
                    .setBranch(branch)
                    .call();
        }

        git.close();
        logger.info("Repository cloned successfully");
        return tempDir.toFile();
    }

    /**
     * List all remote branches in a Git repository
     *
     * @param repoUrl Git repository URL
     * @param token GitHub token for authentication (can be null for public repos)
     * @return List of branch names (without refs/heads/ prefix)
     * @throws GitAPIException if Git operation fails
     */
    public static List<String> listBranches(String repoUrl, String token) throws GitAPIException {
        logger.info("Listing branches for repository: {}", repoUrl);
        
        List<String> branchNames = new ArrayList<>();
        
        try {
            // Create LS-Remote command
            org.eclipse.jgit.api.LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository()
                    .setRemote(repoUrl)
                    .setHeads(true);
            
            if (token != null && !token.isEmpty()) {
                lsRemoteCommand.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider(token, ""));
            }
            
            // Execute the command
            for (Ref ref : lsRemoteCommand.call()) {
                String refName = ref.getName();
                // Remove refs/heads/ prefix to get branch name
                if (refName.startsWith("refs/heads/")) {
                    String branchName = refName.substring("refs/heads/".length());
                    branchNames.add(branchName);
                }
            }
            
            logger.info("Found {} branches", branchNames.size());
            
        } catch (GitAPIException e) {
            logger.error("Error listing branches for repository: {}", repoUrl, e);
            throw e;
        }
        
        return branchNames;
    }

    /**
     * Delete a directory and all its contents
     *
     * @param directory Directory to delete
     * @throws IOException if deletion fails
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete: {}", path, e);
                        }
                    });
            logger.info("Deleted directory: {}", directory);
        }
    }
}
