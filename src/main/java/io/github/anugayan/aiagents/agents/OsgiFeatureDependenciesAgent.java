package io.github.anugayan.aiagents.agents;

import io.github.anugayan.aiagents.utils.GitRepoCloner;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent to extract dependencies from OSGI feature files
 */
public class OsgiFeatureDependenciesAgent {
    private static final Logger logger = LoggerFactory.getLogger(OsgiFeatureDependenciesAgent.class);

    /**
     * Extract all dependencies bundled in OSGI features from a Maven project
     *
     * @param repoUrl Git repository URL
     * @param branch Branch name
     * @param token GitHub token (can be null for public repos)
     * @return Map of feature names to their dependencies
     */
    public Map<String, List<OsgiDependency>> extractFeatureDependencies(String repoUrl, String branch, String token) {
        File repoDir = null;
        try {
            // Clone the repository
            repoDir = GitRepoCloner.cloneRepository(repoUrl, branch, token);

            // Find all feature XML files
            List<File> featureFiles = findFeatureFiles(repoDir);
            logger.info("Found {} feature XML files", featureFiles.size());

            // Extract dependencies from each feature file
            Map<String, List<OsgiDependency>> allDependencies = new HashMap<>();
            for (File featureFile : featureFiles) {
                String featureName = extractFeatureName(featureFile);
                List<OsgiDependency> dependencies = extractDependenciesFromFeature(featureFile);
                if (!dependencies.isEmpty()) {
                    allDependencies.put(featureName, dependencies);
                    logger.info("Feature '{}' has {} dependencies", featureName, dependencies.size());
                }
            }

            return allDependencies;

        } catch (Exception e) {
            logger.error("Error extracting OSGI feature dependencies", e);
            return new HashMap<>();
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
     * Find all feature XML files in a directory recursively
     */
    private List<File> findFeatureFiles(File directory) {
        List<File> featureFiles = new ArrayList<>();
        findFeatureFilesRecursive(directory, featureFiles);
        return featureFiles;
    }

    private void findFeatureFilesRecursive(File directory, List<File> featureFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Skip common non-feature directories
                    if (!file.getName().equals("target") && !file.getName().equals(".git")) {
                        findFeatureFilesRecursive(file, featureFiles);
                    }
                } else if (file.getName().endsWith("-features.xml") || 
                          (file.getName().equals("features.xml"))) {
                    featureFiles.add(file);
                }
            }
        }
    }

    /**
     * Extract feature name from the file path or content
     */
    private String extractFeatureName(File featureFile) {
        try {
            SAXReader reader = new SAXReader();
            // Disable external entity resolution to prevent XXE attacks
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = reader.read(featureFile);
            Element root = document.getRootElement();
            
            // Try to get the first feature name
            List<Element> features = root.elements("feature");
            if (!features.isEmpty()) {
                Element firstFeature = features.get(0);
                String name = firstFeature.attributeValue("name");
                if (name != null) {
                    return name;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract feature name from XML, using file name", e);
        }
        
        // Fallback to file name
        return featureFile.getName().replace("-features.xml", "").replace("features.xml", "features");
    }

    /**
     * Extract dependencies from a feature XML file
     */
    private List<OsgiDependency> extractDependenciesFromFeature(File featureFile) {
        List<OsgiDependency> dependencies = new ArrayList<>();
        
        try {
            SAXReader reader = new SAXReader();
            // Disable external entity resolution to prevent XXE attacks
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = reader.read(featureFile);
            Element root = document.getRootElement();

            // Parse all features
            List<Element> features = root.elements("feature");
            for (Element feature : features) {
                // Extract bundles
                List<Element> bundles = feature.elements("bundle");
                for (Element bundle : bundles) {
                    String bundleText = bundle.getText();
                    OsgiDependency dep = parseMavenUrl(bundleText);
                    if (dep != null) {
                        dependencies.add(dep);
                    }
                }

                // Extract feature dependencies
                List<Element> featureDeps = feature.elements("feature");
                for (Element featureDep : featureDeps) {
                    String featureText = featureDep.getText();
                    OsgiDependency dep = parseMavenUrl(featureText);
                    if (dep != null) {
                        dep.setType("feature");
                        dependencies.add(dep);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing feature file: {}", featureFile.getPath(), e);
        }

        return dependencies;
    }

    /**
     * Parse Maven URL format: mvn:groupId/artifactId/version
     */
    private OsgiDependency parseMavenUrl(String url) {
        if (url == null || !url.startsWith("mvn:")) {
            return null;
        }

        try {
            String coords = url.substring(4); // Remove "mvn:" prefix
            String[] parts = coords.split("/");
            
            if (parts.length >= 3) {
                OsgiDependency dep = new OsgiDependency();
                dep.setGroupId(parts[0]);
                dep.setArtifactId(parts[1]);
                dep.setVersion(parts[2]);
                dep.setType("bundle");
                return dep;
            }
        } catch (Exception e) {
            logger.debug("Could not parse Maven URL: {}", url, e);
        }

        return null;
    }

    /**
     * Class to represent an OSGI dependency
     */
    public static class OsgiDependency {
        private String groupId;
        private String artifactId;
        private String version;
        private String type = "bundle";

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("%s:%s:%s (%s)", groupId, artifactId, version, type);
        }
    }
}
