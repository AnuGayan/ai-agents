# OSGI Feature Extraction Tests

## Overview

This directory contains comprehensive test cases for the `OsgiFeatureDependenciesAgent` class, which extracts dependencies from OSGI features in multiple formats:

1. **Karaf-style feature XML files** (`*-features.xml`, `features.xml`)
2. **Maven POM files** in feature directories (`*.feature/pom.xml`)
3. **p2.inf files** (Eclipse P2 format) - infrastructure ready

## Test Class: OsgiFeatureDependenciesAgentTest

### Test Coverage

The test suite includes 11 test cases that cover:

1. **Feature Extraction Tests**
   - `testExtractFeatureDependenciesFromKaraf()` - Tests extraction from Apache Karaf repository (Karaf-style feature XML files)
   - `testExtractFeatureDependenciesFromCarbonApimgt()` - Tests extraction from wso2/carbon-apimgt repository (Maven POM features)

2. **Validation Tests**
   - `testDependencyStructureValidation()` - Validates that extracted dependencies have proper structure (groupId, artifactId, version, type)
   - `testFeaturesContainBundles()` - Verifies that at least one feature contains bundle dependencies
   - `testFeatureNamesAreMeaningful()` - Ensures feature names are not empty and meaningful

3. **Error Handling Tests**
   - `testInvalidRepositoryUrl()` - Tests handling of non-existent repository URLs
   - `testInvalidBranch()` - Tests handling of non-existent branch names

4. **Unit Tests for OsgiDependency Class**
   - `testOsgiDependencyToString()` - Tests the toString() format
   - `testOsgiDependencyGettersAndSetters()` - Tests all getters and setters
   - `testOsgiDependencyDefaultType()` - Verifies default type is "bundle"

5. **Performance Tests**
   - `testExtractionPerformance()` - Measures and validates extraction performance (must complete within 5 minutes)

### Test Repositories

#### Apache Karaf (https://github.com/apache/karaf)
- **Branch:** main
- **Feature Format:** Karaf-style OSGI feature XML files
- **Purpose:** Tests extraction from traditional Karaf repositories
- **Feature Files:** Contains multiple `*-features.xml` and `features.xml` files with proper Maven URL format (`mvn:groupId/artifactId/version`)
- **Usage:** Used for positive test cases where features are expected to be found

#### WSO2 Carbon APIMGT (https://github.com/wso2/carbon-apimgt)
- **Branch:** master
- **Feature Format:** Maven POM files in feature directories (e.g., `org.wso2.carbon.apimgt.*.feature/pom.xml`)
- **Purpose:** Test repository specified in requirements, demonstrates Maven POM feature extraction
- **Features Found:** 26 features with dependencies extracted from POM files
- **Usage:** Tests the new Maven POM extraction capability

### Supported Feature Formats

#### 1. Karaf-style Feature XML
Files named:
- `*-features.xml` (e.g., `karaf-features.xml`, `apim-features.xml`)
- `features.xml`

Example structure:
```xml
<features name="test" xmlns="http://karaf.apache.org/xmlns/features/v1.3.0">
    <feature name="my-feature" version="1.0.0">
        <bundle>mvn:org.example/example-bundle/1.0.0</bundle>
        <feature>mvn:org.apache.karaf.features/standard/4.3.0/xml/features</feature>
    </feature>
</features>
```

#### 2. Maven POM Features
Files in directories matching `*.feature` pattern:
- `org.wso2.carbon.apimgt.gateway.feature/pom.xml`
- `org.apache.karaf.features.core.feature/pom.xml`

Bundles are extracted from the `carbon-p2-plugin` `<bundles>` configuration section, which specifies the actual JARs that get packed into the feature.

Example:
```xml
<plugin>
    <artifactId>carbon-p2-plugin</artifactId>
    <executions>
        <execution>
            <id>4-p2-feature-generation</id>
            <configuration>
                <bundles>
                    <bundleDef>org.wso2.carbon.apimgt:org.wso2.carbon.apimgt.gateway</bundleDef>
                    <bundleDef>org.wso2.carbon.apimgt:org.wso2.carbon.apimgt.impl</bundleDef>
                    <bundleDef>com.fasterxml.jackson.core:jackson-core</bundleDef>
                    <importBundleDef>org.wso2.carbon.mediation:org.wso2.carbon.rest.api.stub</importBundleDef>
                </bundles>
            </configuration>
        </execution>
    </executions>
</plugin>
```

If no `carbon-p2-plugin` is found, the agent falls back to extracting from the POM's `<dependencies>` section.

#### 3. p2.inf Files (Infrastructure Ready)
Eclipse P2 installation instructions can be supported in future if needed.

### Running the Tests

#### Run all OSGI feature tests:
```bash
mvn test -Dtest=OsgiFeatureDependenciesAgentTest
```

#### Run a specific test:
```bash
mvn test -Dtest=OsgiFeatureDependenciesAgentTest#testExtractFeatureDependenciesFromKaraf
mvn test -Dtest=OsgiFeatureDependenciesAgentTest#testExtractFeatureDependenciesFromCarbonApimgt
```

#### Run all tests in the project:
```bash
mvn test
```

### Test Execution Time

- Individual tests with repository cloning: 1-3 minutes
- Full test suite: ~3-4 minutes (due to Git clone operations)
- Timeout for feature extraction tests: 5 minutes

### Important Notes

1. **Network Dependency**: Tests require internet access to clone GitHub repositories
2. **Multiple Format Support**: The agent now supports both Karaf XML and Maven POM formats simultaneously
3. **Repository Variations**: Different repositories use different feature formats:
   - Apache Karaf: Karaf-style feature XML files
   - WSO2 Carbon APIMGT: Maven POM files in feature directories
   - Mixed repositories: Can contain both formats
   
   The agent handles all formats and combines results.

4. **Branch Names**: Always verify the default branch name of the test repository (some use "main", others use "master")

### Test Results

Latest test run results:
- **Tests run:** 11
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Status:** ✅ BUILD SUCCESS

**Carbon-apimgt extraction:**
- Found: 26 features
- Format: Maven POM with carbon-p2-plugin bundles configuration
- Gateway feature: 47 bundles (from carbon-p2-plugin `<bundles>` section)
- Examples: org.wso2.carbon.apimgt.gateway.feature, org.wso2.carbon.apimgt.rest.api.store.feature, etc.

**Karaf extraction:**
- Found: Feature XMLs with bundle dependencies
- Format: Karaf-style XML
- Examples: framework feature, etc.

### Test Assertions

Each test makes specific assertions:
- Non-null return values
- Proper data structure (Map of feature names to dependency lists)
- Valid dependency attributes (groupId, artifactId, version, type)
- Correct dependency types ("bundle" or other types from POM)
- Graceful error handling (empty results for invalid inputs)
- Performance within acceptable limits

### Future Enhancements

Potential areas for additional test coverage:
- Test with private repositories (requiring authentication tokens)
- Test with repositories containing malformed feature files
- Test parsing of feature dependencies with additional attributes (classifier, type)
- Test p2.inf file parsing when implemented
- Integration tests with actual OSGI runtime
- Test mixed repositories with both Karaf XML and Maven POM features
