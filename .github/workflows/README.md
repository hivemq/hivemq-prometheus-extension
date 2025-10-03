# GitHub Workflows

This directory contains GitHub Actions workflows for the HiveMQ File RBAC Extension.

## Workflows

### check.yml
**Purpose:** Standard CI checks
**Triggers:** All pushes
**Runs on:** Ubuntu
**Actions:**
- Checkout code
- Setup Java 11 (Temurin)
- Run `./gradlew check`

### releaseExtension.yml
**Purpose:** Release automation
**Triggers:** [See workflow file]
**Runs on:** Ubuntu
**Actions:** [See workflow file]

### reproducible-build-test.yml
**Purpose:** Test and verify reproducible builds across platforms and JDK versions
**Triggers:**
- Pull requests to master
- Manual trigger (workflow_dispatch)

**Jobs:**

1. **build-ubuntu (JDK 11.0.21)**
   - Builds extension on Ubuntu latest with JDK 11.0.21
   - Calculates SHA-256 checksums
   - Uploads artifacts

2. **build-macos (JDK 11.0.25)**
   - Builds extension on macOS latest with JDK 11.0.25
   - Calculates SHA-256 checksums
   - Uploads artifacts

3. **verify-reproducibility**
   - Downloads both Ubuntu and macOS builds
   - Compares checksums to ensure they're identical
   - Verifies file permissions (0644 for files, 0755 for directories)
   - **Fails if builds differ across platforms or JDK versions**

4. **verify-same-environment**
   - Builds extension twice on Ubuntu
   - Compares checksums to ensure consistency
   - **Fails if builds differ in the same environment**

**What This Proves:**
- ✅ Builds are reproducible across different operating systems (Ubuntu vs macOS)
- ✅ Builds are reproducible across different JDK patch versions (11.0.21 vs 11.0.25)
- ✅ Builds are reproducible within the same environment
- ✅ File permissions are normalized correctly
- ✅ Timestamps are normalized correctly
- ✅ Your local build should match GitHub builds (even with different JDK patches)

## Viewing Results

After a workflow run:

1. Go to the **Actions** tab in GitHub
2. Select the **Reproducible Build Test** workflow
3. Click on a run to see the results
4. Check the **verify-reproducibility** job to see cross-platform comparison
5. Download artifacts to compare with your local build:
   - `ubuntu-build` - Contains Ubuntu build artifacts and checksums
   - `macos-build` - Contains macOS build artifacts and checksums

## Comparing with Local Builds

To verify your local macOS build matches GitHub:

```bash
# Build locally
./gradlew clean hivemqExtensionZip
shasum -a 256 build/hivemq-extension/*.zip

# Download the macos-build artifact from GitHub Actions
# Then compare:
shasum -a 256 path/to/downloaded/*.zip

# Checksums should be identical!
```

Or use the automated script:

```bash
./verify-reproducible-build.sh
```

## Troubleshooting

If the reproducible build test fails:

1. **Check the verify-reproducibility job logs** - This will show which checksums differ
2. **Review recent changes** - Did you modify build configuration?
3. **Verify local reproducibility** - Run `./verify-reproducible-build.sh` locally
4. **Check permissions** - Ensure no files have unusual permissions committed
5. **Review REPRODUCIBLE_BUILDS.md** - For known limitations and solutions
