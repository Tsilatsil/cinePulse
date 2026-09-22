# CinePulse-Android

CinePulse is an Android movie and TV show tracking app. Users can register and log in securely, discover trending and searched titles via TMDB, save titles to a personal watchlist, and manage their profile — all backed by a custom REST API and a live hosted database.

# Purpose

The digital entertainment landscape is fragmented across services like Netflix, Prime Video, and Disney+, making it hard to track what you've watched or want to watch. CinePulse solves this by combining unified media discovery (via TMDB), personal watchlist tracking, and secure account management into a single Android app.

# Design considerations

Architecture: the app follows a simple layered structure — Compose UI screens call into a data layer split between local storage (Room) and remote APIs (Retrofit), keeping networking, persistence, and UI concerns separate.
Security: passwords are hashed server-side with bcrypt and never stored or transmitted in plain text after the initial HTTPS request. The session token returned after login/register is stored on-device using EncryptedSharedPreferences, backed by the Android Keystore (AES-256-GCM), rather than plain SharedPreferences.
Offline-first watchlist: the watchlist is stored locally in a Room database, so saved titles remain available without a network connection and update the UI instantly via Kotlin Flow.
Two APIs, two purposes: TMDB (api.themoviedb.org) is used purely for media discovery (search, trending, posters). A separate custom backend — built with Node.js, Express, and MongoDB, and hosted on Railway — handles everything specific to CinePulse users: registration, login, and settings.

# Tech stack

Kotlin · Jetpack Compose · Room · Retrofit · Coroutines/Flow · AndroidX Security Crypto (EncryptedSharedPreferences) · Coil · TMDB API · Node.js/Express · MongoDB Atlas · Railway · GitHub Actions

# Project setup

Clone this repo and open it in Android Studio.
Get a free TMDB API key from https://www.themoviedb.org/settings/api
Add it to local.properties (this file is gitignored, so your key stays private):
TMDB_API_KEY=your_key_here
Sync Gradle and run on an emulator or physical device (minSdk 26).
The app is already pointed at the live backend (https://cinepulse-backend-production.up.railway.app/). To run your own backend instance instead, see the CinePulse-Backend repository and update the base URL in RetrofitInstance.kt.

# Features implemented in this prototype

Register / Login with JWT-based authentication against a custom backend
Passwords hashed server-side (bcrypt); session token encrypted on-device
Persistent login session (skips login screen once authenticated)
Change display name (settings), synced to the backend
Logout
TMDB-powered search and trending discovery, with poster images
Add/remove titles from a personal watchlist (Room, offline-capable, live-updating UI)
Episode-level tracking for TV shows — planned for the final PoE
Reviews and ratings — planned for the final PoE
Gamified watch streaks/badges — planned for the final PoE

# GitHub Actions

.github/workflows/android.yml.
•
Whenever you commit and push your changes to GitHub, this workflow will automatically spin up an environment to:
i.
Checkout your code.
ii.
Set up JDK 17 with built-in Gradle caching for fast builds.
iii.
Grant execution permissions to the Gradle wrapper (gradlew).
iv.
Run your entire suite of local unit tests (./gradlew testDebugUnitTest).

# Testing



# Backend

The REST API this app talks to lives in a separate repository: CinePulse-Backend — a Node.js/Express API with MongoDB Atlas, deployed on Railway. See that repo's README for endpoint documentation and deployment instructions.

# Demo video
https://youtu.be/yf8DlmSSyIw?si=DSdibIWirAd-Z7aj

# AI tool usage

used Claude (Anthropic) as a step-by-step tutor throughout this project, not as a tool to generate the app for me. For each part of CinePulse — the Kotlin/Compose screens, the Room database, the Node.js/Express backend, and the Gradle/KSP build setup — Claude explained what a piece of code did and why, and I typed it in, ran it, and tested it myself in Android Studio, Postman, and the terminal.

When something broke, I was the one reading the actual error messages and reporting them back; Claude then explained the cause and I applied the fix myself. For example, when Room's KSP annotation processor threw an IllegalStateException: unexpected jvm signature V, I looked at the build log, described it, and — after Claude explained it was a known KSP2 bug affecting suspend DAO functions — I made the fix (removing suspend from the DAO methods and dispatching those calls on Dispatchers.IO myself) and rebuilt until it worked. I also independently deployed the backend to Railway via the CLI after Render and Koyeb didn't work out, troubleshooting the environment variables and IP whitelisting myself along the way.



# References

See docs/Part1-Research-Design.pdf for the full reference list (TMDB API docs, Android Developers architecture guide, Material Design 3, etc.).