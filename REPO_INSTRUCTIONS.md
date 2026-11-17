# GitHub Instructions

## Overview

This repository functions in a relatively simple way. There's 2 main branches, `main` and `dev`.

`main` -> Stable Branch
- The main branch must remain fully functional and stable at all times.
- Never push commits directly to main.
- Only merge code that has been reviewed, tested, and confirmed to be working correctly.
- Changes should only reach main when they are ready for release.

`dev` -> Development Branch
- The dev branch is where approved features and changes are integrated before being merged into main.
- Never push commits directly to dev.
- All changes must be submitted through a pull request (PR).
- Every PR must be reviewed by someone other than the author.
- Reviewers can approve the PR or request changes before it is merged.

## Branch Structure

### Epic Branches

An epic represents a major feature or area of the application that may require multiple smaller features to complete.

Epics will follow this naming format:
`epic/<epic_name>`

### Feature Branches

Feature branches will contain the work for a specific issue or feature from within an epic. You can find these in the Kanban board.

To create a new branch:
`git checkout -b your-new-branch`

Feature branches must follow this naming convention:
`feature/<feature_name>`

**Important**: Always create feature branches from the correct epic branch. For example, a budgeting transaction update should belong to the budgeting epic, not the user-account epic.

## Creating a New Branch

Before creating a feature branch, make sure you are working from the correct epic branch and have pulled the latest changes.

**Step 1: Switch to the Correct Epic Branch**

    git switch epic/<epic_name>

Replace epic/user-account with the epic you are working on.

**Step 2: Pull the Latest Changes**

    git pull

This ensures your local epic branch is up to date and helps reduce potential merge conflicts.

**Step 3: Create Your Feature Branch**

    git switch -c feature/<feature_name>

Replace the example branch name with the name of your feature.

**Remember: Feature branches must be created from their corresponding epic branch, not from main or an unrelated feature branch.**

## PR Guidelines

All changes must be reviewed before they are merged. Contributors must not replace the required independent review with their own approval.

### PR Targets

Pull requests must target the appropriate parent branch:

**Feature branch -> Epic branch**: Feature-level changes must be submitted to their corresponding epic branch.

**Epic branch -> dev**: Once an epic is ready for integration, submit a PR targeting dev.

**dev -> main**: Only tested and approved changes should be merged into main.

### PR Requirements

Every PR must include:
- A clear title describing the change.
- A short description explaining what was changed and why.
- Testing details describing how the changes were verified, where applicable.
- Relevant context to help reviewers understand the implementation.

### Review and Approval

Before a PR can be merged:
- Someone other than the author must review the changes.
- Reviewers should approve the PR if the changes meet the project's requirements.
- If changes are needed, reviewers should leave comments explaining what needs to be updated.
- The author must address requested changes before seeking approval again.
- No changes may be merged without the required independent review and approval.

**Please reach out if you have any questions at any point. -Grace**
