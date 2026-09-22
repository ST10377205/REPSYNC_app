# RepSync – Hybrid Offline-First Fitness Companion

RepSync is an Android fitness application developed using Kotlin. The application is designed to help users plan, manage, record, and monitor their workouts in one place.

RepSync follows a **hybrid offline-first architecture**, allowing users to access and manage locally stored information even when there is no internet connection. When connectivity is available, data can be synchronised with Firebase through a REST API.

---

## 👥 Group Members

- **[NAME 1]**
- **[NAME 2]**
- **[NAME 3]**
- **[NAME 4]**

---

## 📱 About RepSync

RepSync provides users with a simple fitness companion for managing their training activities and monitoring their progress. The application focuses on workout planning, exercise information, tracking, and progress analytics. It is designed to remain fully functional in environments where an internet connection may not always be available.

---

## ✨ Features

### 🔐 User Authentication
- Secure Sign Up and Sign In.
- Password hashing using **BCrypt** for data protection.
- Local user storage (Room DB) with Firebase cloud backup.
- Offline access to account information.

### 👤 User Profile
- Overview of total workouts, streaks, and training volume.
- Tracking of personal records and fitness progress.

### 🌙 Dark & Light Mode
- Functional native theme toggle.
- Selection is saved locally using `SharedPreferences`.

### 🏋️ Workout Plan Generator
- Create routines based on muscle splits (Push, Pull, Legs).
- Customizable muscle group selection.

### 📚 Exercise Library
- Reference catalog with instructions, target muscles, and tips.
- Accessible directly within the application.

### ⏱️ Workout Tracker
- Record exercises, sets, reps, and weight.
- Live session timer for monitoring duration.
- Automatic volume calculation.

### 📊 Progress Analytics
- Visual statistics and training trends.
- Custom `StatsChartView` for data visualization using regression trends.

---

## 🏗️ Application Architecture

RepSync uses a hybrid **offline-first architecture**.

```text
                    ┌──────────────────────┐
                    │   RepSync Android    │
                    │     Application      │
                    └──────────┬───────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
        ┌───────▼────────┐           ┌────────▼─────────┐
        │  Room Database │           │   Retrofit REST  │
        │     SQLite     │           │       API        │
        └───────┬────────┘           └────────┬─────────┘
                │                             │
                │                    ┌────────▼─────────┐
                │                    │ Firebase Realtime│
                │                    │     Database     │
                │                    └──────────────────┘
                │
                └──── Local / Offline Storage
```

---

## 🛠️ Technologies Used

| Technology | Purpose |
| :--- | :--- |
| Kotlin | Main programming language |
| Android Studio | IDE for development |
| Room Database | Local data storage (SQLite) |
| Retrofit2 | REST API communication |
| Gson | JSON conversion |
| Firebase | Cloud storage and backup |
| BCrypt | Secure password hashing |
| Coroutines | Asynchronous operations |

---

## 🌐 REST API & Data Storage

RepSync uses **Retrofit2** to communicate with the **Firebase Realtime Database** through its REST API. Data is handled as JSON and mapped to Kotlin objects using **Gson**.

- **Local Storage:** Room provides the primary data layer for users and workouts, ensuring offline availability.
- **Cloud Storage:** Firebase serves as the remote backup and synchronization point when the device is online.

---

## 🔒 Security

We implement **BCrypt/JBCrypt** hashing for all user passwords to ensure they are never stored in plain text. The architecture ensures sensitive data is cached securely on the device and synchronized only over encrypted HTTPS connections.

---

## 🧪 Offline-First Behaviour

RepSync is designed to be resilient. When offline, previously stored local data remains accessible, new workouts can be recorded, and network requests fail gracefully without causing crashes. Data synchronizes automatically once a connection is re-established.

---

## 🚀 Getting Started

### Requirements
- Android Studio
- Android SDK (API 24+)
- Kotlin
- A Firebase project configuration

### Installation
1. Clone the repository: `git clone [YOUR GITHUB REPOSITORY LINK]`
2. Open the project in Android Studio.
3. Synchronise the Gradle dependencies.
4. Add your `google-services.json` or configure the API URL in `WorkoutApiService`.
5. Run the application on an emulator or device.

---

## 🔗 Submission Links

- **GitHub Repository:** [PASTE YOUR GITHUB LINK HERE]
- **YouTube Demonstration Video:** [PASTE YOUR UNLISTED YOUTUBE VIDEO LINK HERE]
