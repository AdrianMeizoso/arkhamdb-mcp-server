#!/bin/bash

# Release script for ArkhamDB MCP Server
# Usage: ./scripts/release.sh <version>
# Example: ./scripts/release.sh 1.0.1

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if version is provided
if [ -z "$1" ]; then
    echo -e "${RED}Error: Version number required${NC}"
    echo "Usage: ./scripts/release.sh <version>"
    echo "Example: ./scripts/release.sh 1.0.1"
    exit 1
fi

VERSION=$1
TAG="v${VERSION}"

echo -e "${YELLOW}🚀 Preparing release ${TAG}${NC}"

# Check if we're on main branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo -e "${YELLOW}⚠️  Warning: You're on branch '${CURRENT_BRANCH}', not 'main'${NC}"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check for uncommitted changes
if [[ -n $(git status -s) ]]; then
    echo -e "${RED}Error: You have uncommitted changes${NC}"
    git status -s
    exit 1
fi

# Update version in build.gradle.kts
echo -e "${GREEN}📝 Updating version in build.gradle.kts${NC}"
sed -i.bak "s/version = \".*\"/version = \"${VERSION}\"/" build.gradle.kts
rm build.gradle.kts.bak

# Run tests
echo -e "${GREEN}🧪 Running tests${NC}"
./gradlew test

# Build fat JAR
echo -e "${GREEN}🔨 Building fat JAR${NC}"
./gradlew clean shadowJar

# Verify JAR exists
JAR_FILE=$(find build/libs -name "*-all.jar" -type f | head -n 1)
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Error: Fat JAR not found${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Built: $(basename $JAR_FILE)${NC}"
echo -e "${GREEN}📦 Size: $(du -h $JAR_FILE | cut -f1)${NC}"

# Commit version change
echo -e "${GREEN}📝 Committing version change${NC}"
git add build.gradle.kts
git commit -m "Release version ${VERSION}"

# Create and push tag
echo -e "${GREEN}🏷️  Creating tag ${TAG}${NC}"
git tag -a "${TAG}" -m "Release ${VERSION}"

echo -e "${YELLOW}Ready to push!${NC}"
echo -e "The following commands will be executed:"
echo -e "  ${GREEN}git push origin ${CURRENT_BRANCH}${NC}"
echo -e "  ${GREEN}git push origin ${TAG}${NC}"
echo ""
read -p "Push to GitHub and trigger release? (y/N) " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    git push origin ${CURRENT_BRANCH}
    git push origin ${TAG}
    echo -e "${GREEN}✨ Release ${TAG} pushed!${NC}"
    echo -e "${GREEN}🎉 GitHub Actions will build and create the release${NC}"
    echo -e "${GREEN}📦 Check: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:/]\(.*\)\.git/\1/')/releases${NC}"
else
    echo -e "${YELLOW}⚠️  Release not pushed. You can push manually:${NC}"
    echo -e "  git push origin ${CURRENT_BRANCH}"
    echo -e "  git push origin ${TAG}"
    echo -e "${YELLOW}Or rollback:${NC}"
    echo -e "  git tag -d ${TAG}"
    echo -e "  git reset --hard HEAD~1"
fi
