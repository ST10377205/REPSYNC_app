# RepSync – Hybrid Offline-First Fitness Companion

RepSync is a modern Android fitness application developed using Kotlin, designed to help users plan, track, and analyze their workouts in one unified platform. The app is built with a focus on reliability, data security, and seamless synchronization, ensuring users can maintain their fitness journey regardless of network availability.

RepSync follows a **hybrid offline-first architecture**, allowing users to access and manage information locally even without an internet connection. When connectivity is available, data is automatically synchronised with the cloud through secure REST API integrations.

---

## 📱 About RepSync

RepSync provides a comprehensive fitness companion for managing training activities and monitoring progress. The application focuses on workout planning, exercise information, live tracking, and advanced progress analytics. It is designed to remain fully functional in environments where an internet connection may not always be available.

---

## ✨ Features

### 🔐 User Authentication & Smart Validation
- **Secure Access:** Sign Up and Sign In screens with robust input validation to prevent user errors.
- **Strict Input Rules:** The name field is restricted to alphabetic characters (letters and spaces only), and the email field requires a proper format (e.g., user@gmail.com) to ensure data integrity.
- **Data Security:** Implements **BCrypt cryptographic hashing** for all user passwords, ensuring that credentials are never stored in plain text.
- **Hybrid Storage:** Local user storage (Room DB) with instant Firebase cloud backup for seamless cross-device synchronization.

### 👤 User Profile & Settings
- **Activity Summary:** Overview of total workouts, streaks, and training volume milestone tracking.
- **Theme Customization:** Functional native **Dark & Light Mode** toggle found in the Profile/Settings menu. User preferences are saved locally using `SharedPreferences` and persist across application restarts.

### 🏋️ Feature 1: Smart Workout Plan Generator
- Generates structured routines based on muscle splits (Push, Pull, Legs).
- Allows users to customize routines based on their training environment and specific muscle group focus.

### 📚 Feature 2: High-Fidelity Exercise Library
- A comprehensive reference catalog with step-by-step instructions.
- Provides target muscle information (Primary/Secondary) and training tips for every exercise.

### ⏱️ Feature 3: Workout Tracker & Analytics
- **Live Logging:** Record exercises, sets, reps, and weight in real-time with an active session timer.
- **Data Visualization:** Custom `StatsChartView` for rendering training volume trends using linear regression algorithms.
- **Personal Records:** Automatic identification and storage of PRs for every movement.

---

## 🏗️ Application Architecture

RepSync uses a hybrid **offline-first architecture** to ensure stability and performance.




## 🌐 Dual REST API Integration

RepSync utilizes two distinct REST API integrations via **Retrofit2** to handle different data requirements:

1.  **Cloud Sync API (`WorkoutApiService`):** A private REST integration with **Firebase Realtime Database** used for synchronizing user profiles and workout history across devices.
2.  **Public Advice API (`FitnessApiService`):** A third-party integration with `api.adviceslip.com` used to fetch and display live fitness motivation and daily tips on the dashboard.

---

## 🛠️ Technologies Used

| Technology | Purpose |
| :--- | :--- |
| **Kotlin** | Main programming language |
| **Room Database** | Local data storage (SQLite) |
| **Retrofit2** | Dual REST API communication |
| **Gson** | JSON conversion & mapping |
| **Firebase** | Cloud storage and backup |
| **BCrypt** | Secure password hashing |
| **Coroutines** | Asynchronous operations |

---

## 🧪 Quality Assurance & Automated Testing

The project includes a suite of **Automated Unit Tests** to verify core logic and maintain code quality:
- **Validation Testing:** `ValidationUnitTest.kt` ensures the app correctly handles name formatting (letters only) and email validation format.
- **Authentication Logic:** `AuthUnitTest.kt` verifies that the authentication and password hashing loops are functioning as intended.

---

## 🔒 Security

We implement **BCrypt/JBCrypt** hashing for all user passwords to ensure they are never stored in plain text. The architecture ensures sensitive data is cached securely on the device and synchronized only over encrypted HTTPS connections via Retrofit.

---

## 🧪 Offline-First Behaviour

RepSync is designed to be resilient. When offline:
- Previously stored local data remains accessible.
- New workouts can be recorded and saved to the local database.
- Network requests fail gracefully without causing application crashes.
- Data synchronizes automatically once a connection is re-established.

---

## 📌 Project Status

**Status:** Completed

---

## 🚀 Getting Started

### Requirements
- Android Studio
- Android SDK (API 24+)
- A Firebase project configuration (Database URL)

### Installation
1. Clone the repository.
2. Open the project in Android Studio and sync Gradle.
3. Configure your Firebase project and update `WorkoutApiService.kt` with your database URL.
4. Run the application on an emulator or physical device.

---

## 🔗 GITHUB AND YOUTUBE Links

- **GitHub Repository:** [PASTE YOUR GITHUB LINK HERE]
- **YouTube Demonstration Video:** [PASTE YOUR UNLISTED YOUTUBE VIDEO LINK HERE]
