# Release Process

This document describes how to create a new release of the ArkhamDB MCP Server.

## Three Ways to Release

You have three options for creating a release:

1. **GitHub UI (Easiest)** - Click buttons in GitHub Actions
2. **Local Script (Recommended for development)** - Automated script with confirmation
3. **Manual (Full control)** - Step-by-step git commands

---

## Method 1: GitHub UI (Easiest) ⭐

**Best for:** Quick releases, no local setup needed, works from anywhere

### Steps:

1. Go to **Actions** tab on GitHub
2. Select **Manual Release** workflow
3. Click **Run workflow**
4. Enter version (e.g., `1.0.1`)
5. Click **Run workflow**

That's it! GitHub Actions handles everything automatically.

**See:** `.github/MANUAL_RELEASE_GUIDE.md` for detailed instructions.

**Direct link:**
```
https://github.com/<your-username>/arkhamdb-mcp-server/actions/workflows/manual-release.yml
```

---

## Method 2: Local Script (Recommended)

**Best for:** Local development, testing before push, interactive confirmation

### Using the Release Script

The easiest way to create a release locally is using the provided script:

```bash
./scripts/release.sh <version>
```

**Example:**
```bash
./scripts/release.sh 1.0.1
```

This script will:
1. ✅ Verify you're on the correct branch
2. ✅ Check for uncommitted changes
3. ✅ Update the version in `build.gradle.kts`
4. ✅ Run tests
5. ✅ Build the fat JAR
6. ✅ Commit the version change
7. ✅ Create a git tag
8. ✅ Push to GitHub (with confirmation)
9. ✅ Trigger the GitHub Actions release workflow

### What Happens After Pushing

Once you push the tag, GitHub Actions will:
1. Build the project with the new version
2. Run all tests
3. Create a fat JAR with shadowJar
4. Generate release notes from git commits
5. Create a GitHub Release
6. Upload the JAR as a release asset

You can monitor the progress at:
```
https://github.com/<your-username>/arkhamdb-mcp-server/actions
```

---

## Method 3: Manual Release Process

**Best for:** Maximum control, custom workflows, troubleshooting

If you prefer to do it manually or need more control:

### 1. Update Version

Edit `build.gradle.kts`:
```kotlin
version = "1.0.1"  // Change this
```

### 2. Commit and Tag

```bash
# Commit the version change
git add build.gradle.kts
git commit -m "Release version 1.0.1"

# Create the tag
git tag -a v1.0.1 -m "Release 1.0.1"

# Push both commit and tag
git push origin main
git push origin v1.0.1
```

### 3. Wait for GitHub Actions

The release workflow will automatically trigger when you push a tag matching `v*.*.*`.

---

## 🆚 Method Comparison

| Feature | GitHub UI | Local Script | Manual |
|---------|-----------|--------------|--------|
| **Ease of use** | 🟢 Easiest | 🟡 Easy | 🔴 Complex |
| **Setup required** | ❌ None | ✅ Local repo | ✅ Local repo |
| **Works remotely** | ✅ Yes | ❌ No | ❌ No |
| **Confirmation** | ❌ Immediate | ✅ Interactive | ✅ Manual |
| **Rollback** | 🟡 Manual delete | ✅ Built-in | ✅ Full control |
| **Speed** | ⚡ Fast | 🐢 Slower | 🐢 Slower |

**Recommendation:** Use GitHub UI for quick releases, local script for development.

---

## Release Workflow Details

### Trigger
The release workflow (`release.yml`) triggers on:
- Tags matching pattern: `v*.*.*` (e.g., v1.0.0, v1.2.3)

### Steps
1. **Checkout**: Fetches repository code
2. **Setup Java**: Installs JDK 21
3. **Extract Version**: Parses version from tag
4. **Update Version**: Updates `build.gradle.kts` with tag version
5. **Test**: Runs `./gradlew test`
6. **Build**: Runs `./gradlew shadowJar`
7. **Generate Release Notes**: Creates changelog from git commits
8. **Create Release**: Publishes GitHub release with:
   - Release title
   - Auto-generated changelog
   - Installation instructions
   - Fat JAR as downloadable asset

### Artifacts
The workflow produces:
- **GitHub Release**: Visible on the releases page
- **JAR File**: Attached to the release (e.g., `arkhamdb-mcp-server-1.0.1-all.jar`)
- **Build Artifacts**: Stored for 30 days in GitHub Actions

## Version Numbering

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** (1.x.x): Breaking changes
- **MINOR** (x.1.x): New features (backward compatible)
- **PATCH** (x.x.1): Bug fixes

**Examples:**
- `1.0.0` → `1.0.1`: Bug fix
- `1.0.1` → `1.1.0`: New feature (e.g., new tool)
- `1.1.0` → `2.0.0`: Breaking change (e.g., API change)

## Pre-release Checklist

Before creating a release:

- [ ] All tests pass locally (`./gradlew test`)
- [ ] Build succeeds (`./gradlew shadowJar`)
- [ ] README is up to date
- [ ] CHANGELOG or commit messages describe changes
- [ ] No uncommitted changes
- [ ] On main branch (or appropriate release branch)

## Troubleshooting

### "Permission denied" on release.sh
```bash
chmod +x scripts/release.sh
```

### Build fails in GitHub Actions
- Check the Actions tab for error logs
- Ensure JDK 21 is available
- Verify `gradlew` has execute permissions

### Release not created
- Verify tag matches pattern `v*.*.*`
- Check GitHub Actions permissions (Settings → Actions → General)
- Ensure `GITHUB_TOKEN` has write permissions

### JAR not attached to release
- Check build logs for shadowJar task errors
- Verify JAR file exists in `build/libs/`
- Look for upload errors in GitHub Actions logs

## Rollback a Release

If you need to rollback:

```bash
# Delete the local tag
git tag -d v1.0.1

# Delete the remote tag
git push origin :refs/tags/v1.0.1

# Delete the release on GitHub
# Go to: https://github.com/<username>/arkhamdb-mcp-server/releases
# Click the release → Delete
```

## Testing Releases Locally

Before pushing a tag, test the release process locally:

```bash
# Build the fat JAR
./gradlew clean shadowJar

# Find the JAR
ls -lh build/libs/*-all.jar

# Test it works
java -jar build/libs/arkhamdb-mcp-server-1.0.0-all.jar
```

## Post-Release Steps

After a successful release:

1. **Announce**: Update any relevant channels/communities
2. **Submit to Smithery**: Follow [Smithery submission guide](https://smithery.ai/submit)
3. **Update Documentation**: Ensure installation docs reference the new version
4. **Monitor Issues**: Watch for bug reports related to the new release

## Continuous Integration

The `build.yml` workflow runs on every push to `main`/`develop`:
- Builds the project
- Runs tests
- Creates a snapshot JAR (available in Actions artifacts)

Use this to verify changes before creating a release.
