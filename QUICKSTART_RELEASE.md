# Quick Start: Creating Your First Release

## 🚀 TL;DR - Create a Release in 30 Seconds

```bash
./scripts/release.sh 1.0.1
```

That's it! The script handles everything.

---

## 📦 What Was Set Up

Your repository now has:

### 1. **Automated Release Workflow** (`.github/workflows/release.yml`)
   - Triggers when you push a version tag (e.g., `v1.0.1`)
   - Builds, tests, and creates a GitHub Release
   - Attaches the fat JAR automatically

### 2. **CI Build Workflow** (`.github/workflows/build.yml`)
   - Runs on every push to `main`/`develop`
   - Validates builds and tests
   - Creates snapshot artifacts

### 3. **Release Script** (`scripts/release.sh`)
   - Interactive helper script
   - Handles versioning, tagging, and pushing
   - Includes safety checks

### 4. **Documentation**
   - `RELEASING.md`: Detailed release guide
   - `.github/RELEASE_TEMPLATE.md`: Release checklist

---

## 🎯 Your First Release

### Step 1: Commit Your Current Changes

First, let's commit the release automation files:

```bash
git add .github/ scripts/ RELEASING.md QUICKSTART_RELEASE.md
git commit -m "Add automated release workflow"
git push origin main
```

### Step 2: Create Your First Release

```bash
# Create v1.0.0 release (or whatever version you want)
./scripts/release.sh 1.0.0
```

The script will:
1. ✅ Update `build.gradle.kts` version
2. ✅ Run tests
3. ✅ Build the JAR
4. ✅ Commit changes
5. ✅ Create tag `v1.0.0`
6. ✅ Ask for confirmation
7. ✅ Push to GitHub

### Step 3: Watch the Magic

After pushing, go to:
```
https://github.com/<your-username>/arkhamdb-mcp-server/actions
```

You'll see the release workflow running. When complete:
- ✅ Release created at `github.com/<your-username>/arkhamdb-mcp-server/releases`
- ✅ JAR file attached and ready for download
- ✅ Auto-generated release notes

---

## 🔄 Workflow Diagram

```
┌─────────────────┐
│ Run release.sh  │
└────────┬────────┘
         │
         ├─► Update build.gradle.kts
         ├─► Run tests
         ├─► Build JAR
         ├─► Commit + Tag
         └─► Push to GitHub
                  │
                  ▼
         ┌────────────────────┐
         │ GitHub Actions     │
         │ (release.yml)      │
         └─────────┬──────────┘
                   │
                   ├─► Checkout code
                   ├─► Setup Java 21
                   ├─► Run tests
                   ├─► Build shadowJar
                   ├─► Generate notes
                   └─► Create Release
                            │
                            ▼
                   ┌─────────────────┐
                   │ GitHub Release  │
                   │ with JAR file   │
                   └─────────────────┘
```

---

## 🎨 Release Types

### Patch Release (Bug Fix)
```bash
./scripts/release.sh 1.0.1  # 1.0.0 → 1.0.1
```

### Minor Release (New Feature)
```bash
./scripts/release.sh 1.1.0  # 1.0.1 → 1.1.0
```

### Major Release (Breaking Change)
```bash
./scripts/release.sh 2.0.0  # 1.1.0 → 2.0.0
```

---

## 🧪 Test Before Releasing

Always test locally first:

```bash
# Build and test
./gradlew clean test shadowJar

# Find the JAR
ls -lh build/libs/*-all.jar

# Test it runs
java -jar build/libs/arkhamdb-mcp-server-1.0.0-all.jar
```

---

## 📋 Release Checklist

Before running `./scripts/release.sh`:

- [ ] All changes committed
- [ ] Tests pass (`./gradlew test`)
- [ ] Build works (`./gradlew shadowJar`)
- [ ] README up to date
- [ ] On main branch

---

## 🐛 Troubleshooting

### "Permission denied" error
```bash
chmod +x scripts/release.sh
```

### Want to cancel after tagging?
```bash
# Delete local tag
git tag -d v1.0.1

# Reset commit
git reset --hard HEAD~1
```

### Release failed on GitHub?
Check the Actions tab for logs:
```
https://github.com/<your-username>/arkhamdb-mcp-server/actions
```

---

## 🎉 After Release

### 1. Download and Test
Visit your releases page and download the JAR to verify it works.

### 2. Submit to Smithery.ai
Now you can submit your MCP to Smithery! (Next step in your journey)

### 3. Share
Let people know your MCP is available!

---

## 📚 More Details

For detailed information, see `RELEASING.md`

## 💡 Pro Tips

1. **Semantic Versioning**: Use meaningful version numbers
   - MAJOR: Breaking changes
   - MINOR: New features
   - PATCH: Bug fixes

2. **Good Commit Messages**: The release notes are generated from commits
   ```bash
   ✅ Good: "Add Spanish card search support"
   ❌ Bad: "fix stuff"
   ```

3. **Test in Actions First**: The `build.yml` workflow tests on every push
   - Push to `main` and verify it passes
   - Then create the release

4. **Draft Releases**: Edit `release.yml` to create draft releases first
   ```yaml
   draft: true  # Review before publishing
   ```

---

## 🆘 Need Help?

- Check `RELEASING.md` for detailed documentation
- Review GitHub Actions logs
- Check existing releases for examples

Happy Releasing! 🚀
