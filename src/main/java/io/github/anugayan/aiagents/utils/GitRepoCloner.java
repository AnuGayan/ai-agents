package io.github.anugayan.aiagents.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

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
