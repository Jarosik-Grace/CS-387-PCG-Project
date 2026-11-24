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
