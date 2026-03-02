# Manual Release from GitHub UI

This guide shows you how to create a release directly from GitHub's web interface.

## 🚀 Quick Start

1. Go to your repository on GitHub
2. Click **Actions** tab
3. Select **Manual Release** workflow
4. Click **Run workflow** button
5. Enter the version number (e.g., `1.0.1`)
6. Click **Run workflow**

That's it! GitHub Actions will handle the rest.

---

## 📋 Step-by-Step Guide

### 1. Navigate to Actions

Go to:
```
https://github.com/<your-username>/arkhamdb-mcp-server/actions
```

### 2. Find the Manual Release Workflow

In the left sidebar, click on **Manual Release**

### 3. Run the Workflow

Click the **Run workflow** dropdown button (top right)

### 4. Fill in the Parameters

**Version** (required)
- Enter version number in format: `X.Y.Z`
- Examples: `1.0.1`, `1.2.0`, `2.0.0`
- ❌ Don't include the `v` prefix

**Mark as pre-release** (optional)
- Check this box if it's a beta/alpha/RC release
- Leave unchecked for stable releases

### 5. Trigger the Release

Click the green **Run workflow** button

---

## 🎬 What Happens Next

The workflow will automatically:

1. ✅ **Validate** the version format
2. ✅ **Check** if the tag already exists
3. ✅ **Update** `build.gradle.kts` with new version
4. ✅ **Run** all tests
5. ✅ **Build** the fat JAR with shadowJar
6. ✅ **Commit** the version change
7. ✅ **Create** and push the git tag
8. ✅ **Generate** release notes from commits
9. ✅ **Publish** GitHub Release with JAR attached

### Monitoring Progress

- Watch the workflow run in real-time on the Actions page
- Green checkmark = Success ✅
- Red X = Failed ❌ (check logs for details)

### When Complete

The workflow will show a summary with:
- Release version
- JAR filename and size
- Direct link to the GitHub Release

---

## 📦 Release Types

### Stable Release
```
Version: 1.0.1
Pre-release: ☐ Unchecked
```

### Pre-release (Beta/Alpha)
```
Version: 1.1.0-beta.1
Pre-release: ☑ Checked
```

---

## 🛡️ Safety Features

### Validation Checks
- ✅ Version format validation (`X.Y.Z`)
- ✅ Duplicate tag detection
- ✅ Test suite must pass
- ✅ Build must succeed

### Automatic Actions
- 🤖 Bot commits with `github-actions[bot]` user
- 🔖 Creates git tag automatically
- 📝 Generates changelog from commits
- 🚀 Publishes release immediately

---

## 🆚 Comparison with Other Methods

| Feature | GitHub UI | Local Script | Tag Push |
|---------|-----------|--------------|----------|
| **Ease of use** | 🟢 Easiest | 🟡 Easy | 🔴 Manual |
| **No local setup** | ✅ Yes | ❌ No | ❌ No |
| **Works from anywhere** | ✅ Yes | ❌ No | ❌ No |
| **Automation** | ✅ Full | 🟡 Semi | ❌ Manual |
| **Confirmation prompt** | ❌ No | ✅ Yes | ❌ No |
| **Rollback** | 🟡 Manual | ✅ Easy | 🟡 Manual |

---

## 🔧 Troubleshooting

### "Tag already exists"
**Error:** Tag v1.0.1 already exists

**Solution:** Either:
- Use a different version number
- Delete the existing tag (if you're sure)

### "Version format invalid"
**Error:** Version must be in format X.Y.Z

**Solution:** Use semantic versioning:
- ✅ Good: `1.0.1`, `2.0.0`, `1.2.3`
- ❌ Bad: `v1.0.1`, `1.0`, `latest`

### "Tests failed"
**Solution:**
1. Check the test logs in the workflow
2. Fix failing tests locally
3. Push the fix
4. Re-run the workflow

### "Build failed"
**Solution:**
1. Check the build logs
2. Ensure Java 21 compatibility
3. Fix build issues locally
4. Push the fix
5. Re-run the workflow

### "Permission denied"
**Error:** Workflow can't push changes

**Solution:**
1. Go to Settings → Actions → General
2. Under "Workflow permissions"
3. Select "Read and write permissions"
4. Save and re-run workflow

---

## 🎯 Best Practices

### Before Releasing

- [ ] All changes merged to main
- [ ] Tests passing locally
- [ ] CHANGELOG or commit messages updated
- [ ] Version follows semantic versioning
- [ ] No breaking changes without major version bump

### Version Numbering

Follow [Semantic Versioning](https://semver.org/):

- **1.0.0 → 1.0.1**: Bug fixes (PATCH)
- **1.0.1 → 1.1.0**: New features (MINOR)
- **1.1.0 → 2.0.0**: Breaking changes (MAJOR)

### Good Commit Messages

The workflow generates release notes from commits:

✅ **Good:**
```
Add Spanish language support
Fix null pointer in card search
Improve PDF parsing performance
```

❌ **Bad:**
```
fix
update stuff
wip
```

---

## 🔄 Alternative: Scheduled Releases

Want to automate releases on a schedule? Add to the workflow:

```yaml
on:
  workflow_dispatch:  # Keep manual trigger
  schedule:
    - cron: '0 0 * * 0'  # Every Sunday at midnight
```

---

## 📱 Mobile Workflow

The workflow also works from:
- ✅ GitHub mobile app
- ✅ Tablets
- ✅ Any device with GitHub access

Perfect for releasing on the go! 🚀

---

## 🆘 Need Help?

1. Check workflow logs in Actions tab
2. Review `RELEASING.md` for more details
3. Check GitHub Actions documentation
4. Look at previous successful runs for reference

---

## 🎉 After Release

Once the workflow completes:

1. **Verify the Release**
   - Go to Releases page
   - Download and test the JAR

2. **Submit to Smithery**
   - Use the release URL
   - Submit at https://smithery.ai/submit

3. **Announce**
   - Update documentation
   - Notify users/community
   - Share on relevant channels

---

## 💡 Pro Tips

1. **Test in a fork first** - Try the workflow in a test repository
2. **Use draft releases** - Set `draft: true` in workflow for review
3. **Pre-release for testing** - Mark as pre-release for beta versions
4. **Keep commits clean** - Good commit messages = better release notes
5. **Monitor Actions quota** - GitHub has monthly action limits

---

## 📊 Workflow Run Time

Typical workflow duration:
- Small project: 2-5 minutes
- This project: ~10-15 minutes (includes PDF processing)

The workflow will:
- ⚡ Be faster than local (cloud compute)
- 🔄 Show real-time progress
- 📧 Email you when complete (if enabled)
