---
id: allusive-todo
title: TODO
description: Things to DO
slug: /todo
sidebar_position: 3
---

## TODO

Format is (Status) (ADD|FIX|IMP) - (Description) (`commitHash|pullRequestId|issueId`)

### GitHub Cli

#### Issues

Create New Issue in Main Project

`gh issue create --title "[title]" --body "[body]" --project "pointer-replacer" --label (ADD|FIX|IMP)`

### Pending

- [ ] ADD - Support replacing mouse pointer with Magisk method
- [ ] IMP - Detect change in size while repacking magisk module.
  - [ ] use database to store meta data of created magisk modules.
- [ ] IMP - Remove `multiDexKeepFile` and use `multiDexKeepProguard` instead.

### v1.10.2

- [x] IMP - Add branded login screen instead of basic firebase-ui auth screen.
- [x] IMP - Update Design to M3 - #55
- [x] FIX - Magisk Install button not visible in MagiskRepoFragment.kt
