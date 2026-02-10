package io.github.anugayan.aiagents.agents;

import io.github.anugayan.aiagents.utils.GitRepoCloner;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent to find dependency versions in Maven projects
 */
public class MavenDependencyVersionAgent {
    private static final Logger logger = LoggerFactory.getLogger(MavenDependencyVersionAgent.class);

    /**
     * Find the version of a dependency in a Maven project from GitHub
     *
     * @param repoUrl Git repository URL
     * @param branch Branch name
     * @param token GitHub token (can be null for public repos)
     * @param groupId Dependency groupId
     * @param artifactId Dependency artifactId
     * @return Version string or null if not found
     */
    public String findDependencyVersion(String repoUrl, String branch, String token, 
                                       String groupId, String artifactId) {
        File repoDir = null;
        try {
            // Clone the repository
            repoDir = GitRepoCloner.cloneRepository(repoUrl, branch, token);

            // Find all pom.xml files
            List<File> pomFiles = findPomFiles(repoDir);
            logger.info("Found {} pom.xml files", pomFiles.size());

            // Search for the dependency in each pom.xml
            for (File pomFile : pomFiles) {
                String version = findDependencyInPom(pomFile, groupId, artifactId);
                if (version != null) {
                    logger.info("Found dependency {}:{} with version {} in {}", 
                              groupId, artifactId, version, pomFile.getPath());
                    return version;
                }
            }

            logger.warn("Dependency {}:{} not found in any pom.xml", groupId, artifactId);
            return null;

        } catch (Exception e) {
            logger.error("Error finding dependency version", e);
            return null;
        } finally {
            // Clean up cloned repository
            if (repoDir != null) {
                try {
                    GitRepoCloner.deleteDirectory(repoDir);
                } catch (IOException e) {
                    logger.error("Error deleting temporary directory", e);
                }
            }
        }
    }

    /**
     * Find all pom.xml files in a directory recursively
     */
    private List<File> findPomFiles(File directory) {
        List<File> pomFiles = new ArrayList<>();
        findPomFilesRecursive(directory, pomFiles);
        return pomFiles;
    }

    private void findPomFilesRecursive(File directory, List<File> pomFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Skip common non-source directories
                    if (!file.getName().equals("target") && !file.getName().equals(".git")) {
                        findPomFilesRecursive(file, pomFiles);
                    }
                } else if (file.getName().equals("pom.xml")) {
                    pomFiles.add(file);
                }
            }
        }
    }

    /**
     * Find a dependency in a pom.xml file
     */
    private String findDependencyInPom(File pomFile, String groupId, String artifactId) {
        try (FileReader reader = new FileReader(pomFile)) {
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            Model model = mavenReader.read(reader);

            // Check dependencies
            List<Dependency> dependencies = model.getDependencies();
            if (dependencies != null) {
                for (Dependency dep : dependencies) {
                    if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                        return dep.getVersion();
                    }
                }
            }

            // Check dependency management
            if (model.getDependencyManagement() != null && 
                model.getDependencyManagement().getDependencies() != null) {
                for (Dependency dep : model.getDependencyManagement().getDependencies()) {
                    if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                        return dep.getVersion();
                    }
                }
            }

        } catch (IOException | XmlPullParserException e) {
            logger.debug("Error reading pom.xml file: {}", pomFile.getPath(), e);
        }

        return null;
    }
}
