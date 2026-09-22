\# CinePulse 🎬



CinePulse is an Android app for discovering, tracking, and organizing movies and TV shows. It pulls live data from \*\*TMDB\*\*, keeps a personal watchlist and viewing history in sync via \*\*Firebase\*\*, and works offline with a local \*\*Room\*\* cache.



\## Features



\- Browse and search movies \& TV shows via the TMDB API

\- Track watched titles and maintain a personal watchlist

\- Episode-level tracking for TV shows

\- User accounts with Firebase Authentication

\- Cross-device sync via Firebase Firestore, with background sync through WorkManager

\- Offline-first local cache using Room

\- Built entirely with Jetpack Compose and Material 3



\## Tech Stack



| Layer | Technology |

|---|---|

| UI | Jetpack Compose, Material 3, Navigation Compose |

| Local storage | Room |

| Remote data | Retrofit, OkHttp, kotlinx.serialization |

| Auth \& sync | Firebase Authentication, Firebase Firestore |

| Background work | WorkManager |

| Images | Coil |

| Language | Kotlin |



\## Project Structure





\## Getting Started



\### Prerequisites



\- Android Studio (latest stable)

\- JDK 17

\- A \[TMDB API key](https://www.themoviedb.org/settings/api)

\- A Firebase project with Android app configured (Authentication + Firestore enabled)



\### Setup



1\. \*\*Clone the repo\*\*

```bash

&#x20;  git clone https://github.com/Tsilatsil/cinePulse.git

&#x20;  cd cinePulse

```



2\. \*\*Add your TMDB token\*\*



&#x20;  Create a `local.properties` file in the project root (this file is git-ignored) and add:

```properties

&#x20;  TMDB\_TOKEN=your\_tmdb\_api\_token\_here

```



3\. \*\*Add your Firebase config\*\*



&#x20;  Download `google-services.json` from your Firebase console and place it in `app/google-services.json`.



4\. \*\*Build and run\*\*



&#x20;  Open the project in Android Studio and run it, or from the command line:

```bash

&#x20;  ./gradlew assembleDebug

```



\## Roadmap



\- \[ ] Recommendations based on watch history

\- \[ ] Social features (friend activity, shared watchlists)

\- \[ ] Widgets for quick watchlist access



\## License



This project currently has no license specified. Add a `LICENSE` file if you'd like to open-source it under a specific license (e.g. MIT, Apache 2.0).



\## Video

https://youtu.be/yf8DlmSSyIw?si=DSdibIWirAd-Z7aj

