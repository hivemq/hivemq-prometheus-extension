#!/bin/bash
set -e

echo "=========================================="
echo "Reproducible Build Verification Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create temporary directory
TEMP_DIR=$(mktemp -d)
trap "rm -rf ${TEMP_DIR}" EXIT

echo "${YELLOW}Using temporary directory: ${TEMP_DIR}${NC}"
echo ""

# Build twice and compare
echo "${YELLOW}Step 1: First build${NC}"
./gradlew clean hivemqExtensionZip
cd build/hivemq-extension
shasum -a 256 *.zip > "${TEMP_DIR}/checksums1.txt"
shasum -a 256 *.jar >> "${TEMP_DIR}/checksums1.txt"
cd ../..

echo ""
echo "${YELLOW}Step 2: Waiting 2 seconds...${NC}"
sleep 2

echo ""
echo "${YELLOW}Step 3: Second build${NC}"
./gradlew clean hivemqExtensionZip
cd build/hivemq-extension
shasum -a 256 *.zip > "${TEMP_DIR}/checksums2.txt"
shasum -a 256 *.jar >> "${TEMP_DIR}/checksums2.txt"
cd ../..

echo ""
echo "=========================================="
echo "Build Checksums Comparison"
echo "=========================================="
echo ""
echo "First build:"
cat "${TEMP_DIR}/checksums1.txt"
echo ""
echo "Second build:"
cat "${TEMP_DIR}/checksums2.txt"
echo ""

# Compare
if diff "${TEMP_DIR}/checksums1.txt" "${TEMP_DIR}/checksums2.txt" > /dev/null; then
    echo "${GREEN}✅ SUCCESS: Builds are reproducible!${NC}"
    echo ""
else
    echo "${RED}❌ FAILED: Builds differ${NC}"
    echo ""
    diff "${TEMP_DIR}/checksums1.txt" "${TEMP_DIR}/checksums2.txt" || true
    rm -rf "${TEMP_DIR}"
    exit 1
fi

# Verify permissions
echo "=========================================="
echo "Verifying File Permissions"
echo "=========================================="
echo ""
zipinfo -l build/hivemq-extension/*.zip | head -15
echo ""

# Check for correct permissions
if zipinfo -l build/hivemq-extension/*.zip | grep -E '^-' | grep -v '^-rw-r--r--' > /dev/null; then
    echo "${RED}❌ Found files without 0644 permissions${NC}"
    exit 1
fi

if zipinfo -l build/hivemq-extension/*.zip | grep -E '^d' | grep -v '^drwxr-xr-x' > /dev/null; then
    echo "${RED}❌ Found directories without 0755 permissions${NC}"
    exit 1
fi

echo "${GREEN}✅ All permissions are correctly normalized (files: 0644, directories: 0755)${NC}"
echo ""

# Check timestamps
echo "=========================================="
echo "Verifying Timestamps"
echo "=========================================="
echo ""
if zipinfo -l build/hivemq-extension/*.zip | grep -v '80-Feb-01 00:00' | grep -E '^-|^d' > /dev/null; then
    echo "${RED}❌ Found entries with non-normalized timestamps${NC}"
    exit 1
fi

echo "${GREEN}✅ All timestamps are normalized to epoch (80-Feb-01 00:00)${NC}"
echo ""

echo "=========================================="
echo "${GREEN}🎉 All reproducibility checks passed!${NC}"
echo "=========================================="
echo ""
echo "Your builds are:"
echo "  ✓ Reproducible across multiple builds"
echo "  ✓ Using normalized file permissions"
echo "  ✓ Using normalized timestamps"
echo ""
echo "To compare with GitHub Actions builds:"
echo "  1. Push your changes to trigger the workflow"
echo "  2. Download the artifacts from the workflow run"
echo "  3. Compare checksums with your local build"
echo ""
