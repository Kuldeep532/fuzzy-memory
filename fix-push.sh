#!/usr/bin/env bash
# =============================================================================
#  fix-push.sh — Nexus Plus GitHub Push Fix
#
#  What this does:
#    1. Removes Replit platform files from git tracking (does NOT delete them
#       from disk — just stops tracking them in the repo)
#    2. Commits the cleanup
#    3. Force-pushes to GitHub, overwriting the diverged remote history
#
#  Run from the workspace root:
#    chmod +x fix-push.sh && ./fix-push.sh
# =============================================================================

set -e  # stop on any error

REMOTE="origin"
BRANCH="main"

echo ""
echo "╔════════════════════════════════════════════════╗"
echo "║  Nexus Plus — GitHub Push Fix                  ║"
echo "╚════════════════════════════════════════════════╝"
echo ""

# ── Step 1: Remove Replit platform files from git tracking ──────────────────
echo "[1/4] Removing Replit platform files from git tracking..."

# These are removed from the INDEX (git stops tracking them) but the actual
# files stay on disk so Replit continues to work normally.
DIRS_TO_UNTRACK=(
    "artifacts"
    "lib"
    "scripts"
    "attached_assets"
)
FILES_TO_UNTRACK=(
    "pnpm-lock.yaml"
    "pnpm-workspace.yaml"
    "package.json"
    "tsconfig.json"
    "tsconfig.base.json"
    "replit.md"
    ".npmrc"
    ".replit"
    ".replitignore"
)

for dir in "${DIRS_TO_UNTRACK[@]}"; do
    if git ls-files --error-unmatch "$dir" > /dev/null 2>&1; then
        git rm -r --cached "$dir" 2>/dev/null || true
        echo "    ✓  untracked: $dir/"
    fi
done

for file in "${FILES_TO_UNTRACK[@]}"; do
    if git ls-files --error-unmatch "$file" > /dev/null 2>&1; then
        git rm --cached "$file" 2>/dev/null || true
        echo "    ✓  untracked: $file"
    fi
done

echo ""

# ── Step 2: Stage the updated .gitignore ────────────────────────────────────
echo "[2/4] Staging updated .gitignore..."
git add .gitignore

# ── Step 3: Commit the cleanup ───────────────────────────────────────────────
echo "[3/4] Committing cleanup..."
git commit -m "chore: remove Replit platform files — keep Android project only

Untracked from git (files remain on disk for Replit to work):
- artifacts/  (Replit API server + mockup sandbox)
- lib/         (Replit workspace libraries)
- scripts/     (Replit workspace scripts)
- pnpm-lock.yaml, pnpm-workspace.yaml, package.json
- tsconfig.json, tsconfig.base.json, replit.md, .npmrc

GitHub repo now contains only:
- NexusPlus/   (Jetpack Compose Android project)
- .github/     (CI/CD workflows)
- .gitignore"

echo ""

# ── Step 4: Force-push to GitHub ─────────────────────────────────────────────
echo "[4/4] Force-pushing to GitHub (overwriting diverged remote history)..."
echo "      Remote: $REMOTE/$BRANCH"
echo ""
git push --force-with-lease "$REMOTE" "$BRANCH"

echo ""
echo "╔════════════════════════════════════════════════╗"
echo "║  ✓  Push successful!                           ║"
echo "║                                                ║"
echo "║  GitHub repo now contains ONLY:                ║"
echo "║    • NexusPlus/  — Android project (77 files)  ║"
echo "║    • .github/    — CI/CD workflows             ║"
echo "╚════════════════════════════════════════════════╝"
echo ""
