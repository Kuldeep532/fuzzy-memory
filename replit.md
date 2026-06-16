# Nexus Plus — Android App Showcase

## Overview
This is the source code repository for **Nexus Plus**, a multi-utility Android super-app by Nexus Wave Technologies. It contains 40 features across Media, Productivity, Utilities, Smart Tools, and Security categories.

Since Replit cannot run Android apps natively, this project includes a **web landing page** that showcases the app's features and provides a code browser.

## Running the App
The web showcase runs on port 5000 using a simple Node.js static server.

## Android App Setup
To build and run the actual Android app:
1. Open `NexusPlus/` in Android Studio (Hedgehog 2023.1.1+)
2. Wait for Gradle sync
3. Run on a device or emulator (API 26+)

## Project Structure
- `NexusPlus/` — Android app source code (Kotlin, Gradle)
- `index.html` — Web landing page / feature showcase
- `server.js` — Static file server

## User Preferences
- Keep the web showcase in sync with app features
