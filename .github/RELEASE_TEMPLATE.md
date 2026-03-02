---
name: Release Checklist
about: Checklist for creating a new release
---

## Release Version: v_._._

### Pre-Release
- [ ] All tests passing
- [ ] README.md is up to date
- [ ] Version number follows semantic versioning
- [ ] No uncommitted changes
- [ ] On main branch

### Release Process
- [ ] Run `./scripts/release.sh <version>`
- [ ] Verify GitHub Actions build succeeds
- [ ] Check release is created on GitHub
- [ ] Download and test the JAR file

### Post-Release
- [ ] Submit to Smithery.ai
- [ ] Update any external documentation
- [ ] Announce release (if applicable)

### Notes
<!-- Add any release-specific notes here -->
