#!/bin/bash
set -e

# Post-merge setup for Nexus Plus.
# This is a pure Android/Kotlin project — all dependencies are managed by Gradle
# inside Android Studio. There are no npm packages, database migrations, or
# server-side steps to run in the Replit environment.
echo "Post-merge setup complete (no-op for Android project)."
