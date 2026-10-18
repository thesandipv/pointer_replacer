# Agent Instructions & Guidelines

This document consolidates instructions, architectural rules, and development preferences provided by the user across past development sessions for the **Pointer Replacer** project ecosystem.

---

## 1. General Agent Guidelines

- **Tool Usage**:
  - Always prefer dedicated agent tools (`view_file`, `replace_file_content`, `write_to_file`, etc.) over shell commands (`cat`, `sed`, `echo`, `head`, `tail`) when inspecting or modifying files.
  - Use bash commands only when necessary (e.g., running builds, tests, git operations).
- **Communication & Verbosity**:
  - Keep responses, terminal outputs, and script print statements concise. Avoid excessive verbosity.
- **Changelog & Documentation**:
  - Log significant updates in the relevant changelog after completing changes.
  - Preserve documentation integrity and existing comments.
- **Git & Pull Requests**:
  - Use GitHub CLI (`gh`) for creating or managing pull requests.
  - Keep PR titles and descriptions clean, focused, and minimal.
- **Planning & Verification**:
  - When asked to plan, research thoroughly, produce an implementation plan artifact, and await explicit approval before modifying code.
  - Always verify changes with builds or tests before concluding a task.

---

## 2. Android App (`allusive`)

### Distribution Variants & Play Store Compliance
- Maintain separate product flavors:
  - **`play` (Google Play Store)**:
    - Must strictly comply with Google Play Developer policies.
    - No direct APK modification or local installation of Magisk/RRO overlay modules.
    - Only Xposed is allowed directly within the Play flavor.
    - Provide a button to download RRO modules from the website instead of packaging/installing them in-app.
    - Redirect to the website using the **Firestore Document ID** (`/pointer/<docId>`), **not** raw filenames (`RepoNavigator.kt`).
  - **`github` (Full Variant)**:
    - Includes root/Magisk module packaging and direct RRO capabilities.
- **Deprecated Features**: The legacy `framework-res` method must remain removed.

### Navigation & Architecture
- Planning to use compose navigation.

### UI & System Insets (Edge-to-Edge)
- Always enable edge-to-edge via `enableEdgeToEdge()` in activities (`MainActivity`, `OnboardingActivity`).
- Use `ViewCompat.setOnApplyWindowInsetsListener` to dynamically handle `WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()`.
- **CoordinatorLayout / Dashboard**:
  - Avoid `fitsSystemWindows="true"` on top-level child `AppBarLayout` if it consumes insets and starves sibling views.
  - Dynamically pad the toolbar/title layout for top/left/right insets, and the bottom navigation for bottom/left/right insets.
  - Dynamically calculate `fragment_repo_nav` bottom margin and `fab_apply` bottom margin based on whether the bottom navigation bar is visible, its height, and the system bottom inset. Never use hardcoded bottom margins (e.g., static 80dp).
- **Scrolling Containers**:
  - Set `android:clipToPadding="false"` and appropriate bottom padding (e.g. `80dp`) on `RecyclerView` and `NestedScrollView` so content can be scrolled fully above bottom navigation and FABs.

### Authentication & Guest Mode
- Make login optional on app launch: users should be able to enter directly in Guest Mode.
- Restrict only authenticated actions (e.g., posting new pointers, user profile settings).

### Pointer Upload Validation
- Validate image format: required format is **PNG**.
- Check for transparency: reject images without an alpha channel.
- Automatically resize/normalize pointer dimensions before uploading to Firebase Storage.

---

## 3. RRO Generation & Packaging (`arro`)

- **Submodule Location**: All RRO building, compiling, and packaging logic resides in the `arro` directory/submodule.
- **RRO 2.0 (Zip Distribution)**:
  - Distribute Magisk-flashable ZIP packages rather than standalone raw APKs.
- **Pointer Types**:
  - Separate touch pointer overlays (`pointer_spot_touch`) and mouse pointer overlays (`pointer_spot_hover` / mouse) into distinct packages.
- **Module Metadata**:
  - `module.prop` must store human-readable pointer names from Firestore, **not** raw file names.
- **Processing Scripts**:
  - Keep script names aligned with their specific purpose (e.g., `compile_rro.py`, `process_requested_rros.py`).
  - `process_requested_rros.py` must only process requested RROs where `isRequestClosed == false` in `arro/data/pointers.json`.
  - Minimize stdout verbosity during batch generation and compile runs.

---

## 4. Website Frontend (`website`)

- **Branch**: New website development is based on the `sv/new_website` branch.
- **Tech Stack**:
  - Modernized with **SvelteKit**.
  - Package manager: Always use **`pnpm`**.
- **Design & Styling**:
  - Use the latest version of **Tailwind CSS**.
  - Modern UI styling with **glassmorphism** effects.
  - Properly calibrated dark mode palette (ensure contrast and theme consistency).
- **Icons**:
  - Use `@lucide/svelte` (do **not** use the legacy `lucide-svelte` package).
- **URL Routing**:
  - Link to pointer detail and download pages using the Firestore Document ID.

---

## 5. Backend & Firebase Services

- **Firebase Emulators**: Use Firebase Emulators for testing and local validation.
- **Account Deletion & Cascading Cleanups**:
  - When deleting a user account, cascade the deletion to their uploaded pointers and pointer requests.
  - Maintain an audit record of deleted user UIDs.
- **Content Moderation**:
  - Enforce moderation in the pointer repository to detect/prevent duplicate uploads and inappropriate or explicit content.
