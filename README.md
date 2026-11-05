CinePulse – Movie Discovery Android Application
Project Overview

CinePulse is an Android movie discovery application designed to provide users with an engaging and convenient movie browsing experience. The application allows users to explore movies, manage their watchlists and access movie information.

The Part 3 implementation builds on the previous version by introducing offline synchronisation, Single Sign-On (SSO), notifications and multilingual support.

Key Features
Movie Discovery: Browse and discover movies.
Trending Movies: Explore popular and trending content.
Watchlist: Save and manage movies for later viewing.
Movie Details: View information about selected movies.
Offline Synchronisation: Support access and synchronisation of relevant application data when connectivity is restored.
Single Sign-On (SSO): Simplified authentication.
Notifications: Keep users informed through application notifications.
Multilingual Support: Support multiple languages.
User Profile: Access user-related information and settings.
Technologies Used
Kotlin
Android Studio
Gradle
Firebase
TMDB API
Git and GitHub
GitHub Actions
Installation and Setup
Clone the repository:
bash
git clone https://github.com/Tsilatsil/cinePulse.git

Open the project in Android Studio.
Allow Gradle to synchronise.
Configure the required TMDB API key in your local local.properties file.
Build and run the application using an Android emulator or compatible Android device.
Automated Testing

The project uses Gradle for automated unit testing.

To execute the unit tests locally, run:

bash
./gradlew testDebugUnitTest


GitHub Actions is used to automate the testing process whenever changes are pushed to the repository or a pull request is created.




GitHub: https://github.com/Tsilatsil/cinePulse

