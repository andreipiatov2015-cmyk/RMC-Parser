# Development Workflow

This document defines the mandatory development workflow for the RMC Framework project.

## Branching Rules

### Never
- Never continue working in an old branch
- Never reuse a merged Pull Request
- Never reopen an old Pull Request
- Never continue committing to a closed branch

### Always
- Every task MUST start from the latest `main` branch
- For every task create a NEW branch

### Branch Naming Convention
```
task-{NUMBER}-{short-description}
```

### Examples
- `task-001-framework`
- `task-002-version-engine`
- `task-003-update-dialog`
- `task-004-downloader`
- `task-005-installer`

## Pull Request Rules

### One Task = One Pull Request
Each task must have exactly one Pull Request.

### After Merge
1. Delete the feature branch
2. Create a new branch from `main` for the next task

## Commit Messages

Use meaningful commit messages that describe what was done.

### Good Examples
- "Create Version Engine"
- "Implement JSON downloader"
- "Add Update Dialog"
- "Fix logging"
- "Add version comparison logic"

### Bad Examples (Do Not Use)
- "Update"
- "Fix"
- "Changes"
- "WIP"
- "asdf"

## Repository Rules

### Main Branch
- The `main` branch must always be buildable
- Only merge code that compiles successfully
- No broken builds on main

### Pull Requests
- Every Pull Request must compile successfully before submission
- Run `mvn clean compile` locally before pushing

## Deliverables

After every completed task, provide:

1. **Branch name** - The feature branch created for the task
2. **Commit hash** - The final commit hash
3. **Pull Request link** - Link to the GitHub PR
4. **Modified files** - List of all files changed
5. **Build status** - GitHub Actions status (must be green)
6. **Manual testing instructions** - How to test the feature

## Documentation

All workflow rules are stored in this file: `docs/DEVELOPMENT_WORKFLOW.md`

All future tasks must follow this workflow.

## Quick Reference

### Starting a New Task
```bash
# Ensure main is up to date
git checkout main
git pull origin main

# Create new branch
git checkout -b task-{NUMBER}-{description}

# Implement the feature
# ... work ...

# Commit with meaningful message
git commit -m "Description of what was implemented"

# Push and create PR
git push -u origin task-{NUMBER}-{description}
```

### After PR is Merged
```bash
# Switch to main
git checkout main
git pull origin main

# Delete old branch
git branch -d task-{NUMBER}-{description}
git push origin --delete task-{NUMBER}-{description}

# Create new branch for next task
git checkout -b task-{NEXT_NUMBER}-{description}
```

## Task History

| Task | Branch | PR | Status |
|------|--------|-----|--------|
| TASK-001 | task-001-framework | [#1](https://github.com/andreipiatov2015-cmyk/RMC-Parser/pull/1) | Merged |
| TASK-001.5 | task-001-framework | [#1](https://github.com/andreipiatov2015-cmyk/RMC-Parser/pull/1) | Merged |
| TASK-002 | task-002-* | - | Pending |