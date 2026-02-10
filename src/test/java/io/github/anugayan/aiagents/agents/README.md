# OSGI Feature Extraction Tests

## Overview

This directory contains comprehensive test cases for the `OsgiFeatureDependenciesAgent` class, which extracts dependencies from OSGI feature XML files in Maven repositories.

## Test Class: OsgiFeatureDependenciesAgentTest

### Test Coverage

The test suite includes 11 test cases that cover:

1. **Feature Extraction Tests**
   - `testExtractFeatureDependenciesFromKaraf()` - Tests extraction from Apache Karaf repository (which contains Karaf-style feature XML files)
   - `testExtractFeatureDependenciesFromCarbonApimgt()` - Tests extraction from wso2/carbon-apimgt repository as specified in requirements (demonstrates graceful handling of repositories that use different feature formats like p2.inf)

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
- **Purpose:** Primary test repository that contains actual Karaf-style OSGI feature XML files
- **Feature Files:** Contains multiple `*-features.xml` and `features.xml` files with proper Maven URL format (`mvn:groupId/artifactId/version`)
- **Usage:** Used for positive test cases where features are expected to be found

#### WSO2 Carbon APIMGT (https://github.com/wso2/carbon-apimgt)
- **Branch:** master (note: not "main" as originally specified, as the repository uses "master")
- **Purpose:** Test repository specified in requirements
- **Feature Format:** Uses Maven feature projects with `p2.inf` files rather than traditional Karaf feature XML files
- **Usage:** Demonstrates that the agent gracefully handles repositories that don't contain Karaf-style feature files (returns empty results without errors)

### OSGI Feature XML Format

The agent looks for files named:
- `*-features.xml` (e.g., `karaf-features.xml`, `apim-features.xml`)
- `features.xml`

Example feature XML structure:
```xml
<features name="test" xmlns="http://karaf.apache.org/xmlns/features/v1.3.0">
    <feature name="my-feature" version="1.0.0">
        <bundle>mvn:org.example/example-bundle/1.0.0</bundle>
        <feature>mvn:org.apache.karaf.features/standard/4.3.0/xml/features</feature>
    </feature>
</features>
```

The agent parses:
- **Bundle dependencies:** `<bundle>` elements containing Maven URLs
- **Feature dependencies:** `<feature>` elements containing Maven URLs or feature names

### Running the Tests

#### Run all OSGI feature tests:
```bash
mvn test -Dtest=OsgiFeatureDependenciesAgentTest
```

#### Run a specific test:
```bash
mvn test -Dtest=OsgiFeatureDependenciesAgentTest#testExtractFeatureDependenciesFromKaraf
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
2. **Repository Variations**: Different repositories may use different feature formats:
   - Karaf-style feature XML files (traditional OSGI)
   - Maven p2.inf files (Eclipse P2/OSGi)
   - No feature files at all
   
   The agent handles all cases gracefully by returning an empty map when no feature files are found.

3. **Branch Names**: Always verify the default branch name of the test repository (some use "main", others use "master")

### Test Assertions

Each test makes specific assertions:
- Non-null return values
- Proper data structure (Map of feature names to dependency lists)
- Valid dependency attributes (groupId, artifactId, version, type)
- Correct dependency types ("bundle" or "feature")
- Graceful error handling (empty results for invalid inputs)
- Performance within acceptable limits

### Future Enhancements

Potential areas for additional test coverage:
- Test with private repositories (requiring authentication tokens)
- Test with repositories containing malformed feature XML files
- Test parsing of feature dependencies with additional attributes (classifier, type)
- Test handling of feature file directories outside standard Maven structure
- Integration tests with actual OSGI runtime
