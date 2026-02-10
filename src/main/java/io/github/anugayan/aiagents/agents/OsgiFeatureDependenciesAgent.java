package io.github.anugayan.aiagents.agents;

import io.github.anugayan.aiagents.utils.GitRepoCloner;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent to extract dependencies from OSGI features in multiple formats:
 * - Karaf-style feature XML files
 * - Maven POMs in feature directories
 * - p2.inf files (Eclipse P2 format)
 */
public class OsgiFeatureDependenciesAgent {
    private static final Logger logger = LoggerFactory.getLogger(OsgiFeatureDependenciesAgent.class);
    
    // Constants for dependency handling
    private static final String UNSPECIFIED_VERSION = "unspecified";
    private static final Set<String> BUNDLE_TYPES = new HashSet<>();
    
    static {
        BUNDLE_TYPES.add("jar");
        BUNDLE_TYPES.add("war");
        BUNDLE_TYPES.add(null); // Default Maven type
    }

    /**
     * Extract all dependencies bundled in OSGI features from a Maven project
     * Supports multiple formats: Karaf XML, Maven POMs, and p2.inf files
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

            Map<String, List<OsgiDependency>> allDependencies = new HashMap<>();
            
            // Extract from Karaf-style feature XML files
            List<File> karafFiles = findKarafFeatureFiles(repoDir);
            logger.info("Found {} Karaf-style feature XML files", karafFiles.size());
            for (File featureFile : karafFiles) {
                String featureName = extractKarafFeatureName(featureFile);
                List<OsgiDependency> dependencies = extractDependenciesFromKarafFeature(featureFile);
                if (!dependencies.isEmpty()) {
                    allDependencies.put(featureName, dependencies);
                    logger.info("Feature '{}' has {} dependencies from Karaf XML", featureName, dependencies.size());
                }
            }
            
            // Extract from Maven POMs in feature directories
            List<File> featurePoms = findFeaturePoms(repoDir);
            logger.info("Found {} feature POMs", featurePoms.size());
            for (File pomFile : featurePoms) {
                String featureName = extractPomFeatureName(pomFile);
                List<OsgiDependency> dependencies = extractDependenciesFromPom(pomFile);
                if (!dependencies.isEmpty()) {
                    allDependencies.put(featureName, dependencies);
                    logger.info("Feature '{}' has {} dependencies from POM", featureName, dependencies.size());
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
     * Find all Karaf-style feature XML files in a directory recursively
     */
    private List<File> findKarafFeatureFiles(File directory) {
        List<File> featureFiles = new ArrayList<>();
        findKarafFeatureFilesRecursive(directory, featureFiles);
        return featureFiles;
    }

    private void findKarafFeatureFilesRecursive(File directory, List<File> featureFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Skip common non-feature directories
                    if (!file.getName().equals("target") && !file.getName().equals(".git")) {
                        findKarafFeatureFilesRecursive(file, featureFiles);
                    }
                } else if (file.getName().endsWith("-features.xml") || 
                          (file.getName().equals("features.xml"))) {
                    featureFiles.add(file);
                }
            }
        }
    }
    
    /**
     * Find all feature POMs in the repository
     * Looks for POMs in directories matching *.feature pattern under features/
     */
    private List<File> findFeaturePoms(File directory) {
        List<File> featurePoms = new ArrayList<>();
        findFeaturePomsRecursive(directory, featurePoms);
        return featurePoms;
    }
    
    private void findFeaturePomsRecursive(File directory, List<File> featurePoms) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Skip common non-feature directories
                    if (file.getName().equals("target") || file.getName().equals(".git")) {
                        continue;
                    }
                    
                    // Check if this is a feature directory (ends with .feature)
                    if (file.getName().endsWith(".feature")) {
                        File pomFile = new File(file, "pom.xml");
                        if (pomFile.exists() && pomFile.isFile()) {
                            featurePoms.add(pomFile);
                        }
                    }
                    
                    // Continue recursing for nested feature directories
                    findFeaturePomsRecursive(file, featurePoms);
                }
            }
        }
    }

    /**
     * Extract feature name from Karaf feature file path or content
     */
    private String extractKarafFeatureName(File featureFile) {
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
     * Extract feature name from POM file
     */
    private String extractPomFeatureName(File pomFile) {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pomFile));
            
            // Use artifactId as feature name
            String artifactId = model.getArtifactId();
            if (artifactId != null) {
                return artifactId;
            }
        } catch (Exception e) {
            logger.debug("Could not extract feature name from POM {}, using parent directory name", pomFile.getPath(), e);
        }
        
        // Fallback to parent directory name
        return pomFile.getParentFile().getName();
    }

    /**
     * Extract dependencies from a Karaf feature XML file
     */
    private List<OsgiDependency> extractDependenciesFromKarafFeature(File featureFile) {
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
            logger.error("Error parsing Karaf feature file: {}", featureFile.getPath(), e);
        }

        return dependencies;
    }
    
    /**
     * Extract dependencies from a Maven POM file in a feature directory
     */
    private List<OsgiDependency> extractDependenciesFromPom(File pomFile) {
        List<OsgiDependency> dependencies = new ArrayList<>();
        
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pomFile));
            
            // Extract dependencies
            List<Dependency> pomDependencies = model.getDependencies();
            if (pomDependencies != null) {
                for (Dependency dep : pomDependencies) {
                    OsgiDependency osgiDep = new OsgiDependency();
                    osgiDep.setGroupId(dep.getGroupId());
                    osgiDep.setArtifactId(dep.getArtifactId());
                    osgiDep.setVersion(dep.getVersion() != null ? dep.getVersion() : UNSPECIFIED_VERSION);
                    
                    // Determine type based on packaging or scope
                    String type = dep.getType();
                    if (BUNDLE_TYPES.contains(type)) {
                        osgiDep.setType("bundle");
                    } else {
                        osgiDep.setType(type);
                    }
                    
                    dependencies.add(osgiDep);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error parsing POM file: {}", pomFile.getPath(), e);
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
