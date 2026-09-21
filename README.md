# RepSync - Hybrid Offline-First Fitness Companion

RepSync is a modern Android application designed for fitness enthusiasts to generate custom workout plans, track live training sessions, analyze progression using linear regression charts, and store records reliably with a dual layer architecture.

---

## 📱 Core Features & Functionality

1. **Secure Authentication**
   * Fully implemented **Sign Up** and **Sign In** screens.
   * Leverages **BCrypt cryptographic password hashing** on the client side for superior data security.
   * Features a hybrid authentication loop: stores users locally via Room DB and backs up records instantly to the cloud. Supports cross-device profile migration dynamically.

2. **Personalized Profile & Dark Theme Settings**
   * Customizable user profile dashboard tracking total workouts, running streak data, and volume milestones.
   * Fully functional native **Dark Mode / Light Mode theme toggle** that stores preferences persistently across device reboots via SharedPreferences.

3. **User Defined Feature 1: AI Workout Plan Generator**
   * Tailor-made exercise routine synthesis engine allowing users to choose targeted muscle splits (e.g., Push Day, Pull Day, Leg Day) and training location constraints.

4. **User Defined Feature 2: High-Fidelity Exercise Reference Library**
   * Comprehensive indexed catalog containing step-by-step biomechanical descriptions, target primary/secondary musculature, and pro-tips for common compound and isolation exercises.

5. **User Defined Feature 3: Live Session Tracker & Analytics Dashboard**
   * Real-time workout duration timer loop tracking intensity.
   * Beautiful custom statistical canvas (`StatsChartView`) that renders computed volume trends over time using linear regression algorithms.
   * Persistent tracking of individual personal records (PRs) per movement.

---

## ☁️ Architecture & Backend (REST API + Firebase Cloud)

RepSync implements an advanced **Offline-First + Cloud-Backup** hybrid storage architecture designed to operate seamlessly in any network environment without crashing.

* **REST API Client Framework:** Powered by **Retrofit2** and **Gson Converter** mapping to endpoints inside `WorkoutApiService.kt`.
* **Serverless Backend Infrastructure:** Interacts directly with the official cloud via **Firebase Realtime Database's Native REST API Engine** (`https://repsync-6537c-default-rtdb.firebaseio.com/`).
* **Robust Offline Continuity:** Powered by an offline **Room SQLite Database Cache** layer. If the mobile device loses connectivity or enters airplane mode, all network requests fail gracefully inside resilient background coroutine `try-catch` structures. Data is preserved completely offline and functions identically without service interruption.

---

## 🤖 AI Tool Utilization Write-Up (Rubric Requirement)

* **Tools Used:** Android Studio AI Assistant / Large Language Model Integration.
* **Role of AI:** Assisted in designing safe network layer `try-catch` wrapper configurations, optimizing the custom graph regression arithmetic canvas drawing logic, and adapting Retrofit data mapping parameters to align cleanly with Firebase Realtime Database's key-value pair REST requirements.
* **Verification & Citation:** All AI-suggested code blocks were thoroughly scrutinized, code-inspected using Android Studio's inspection layout toolkit, verified to ensure compiler compliance (`:app:assembleDebug`), and modified to retain style consistency across the application architecture.

---

## 📹 Demonstration Video Link

* **YouTube Video Link:** `[PASTE YOUR UNLISTED YOUTUBE VIDEO LINK HERE]`
* *Note: Ensure your video demonstrates signing up, theme toggling, running a workout session, saving, checking the progress graph feed, and shows the data appearing live inside the Firebase database console browser dashboard.*

---

## 🛠️ Tech Stack & Dependencies

* **Language:** 100% Kotlin
* **UI Pattern:** XML Layout Components with Material Design 3 guidelines
* **Local Database:** Room Persistence Library (SQLite abstraction)
* **Networking & JSON Parsing:** Retrofit2 & Google Gson
* **Security & Cryptography:** JBCrypt Password Hashing Provider
* **Asynchronous Concurrency:** Kotlin Coroutines & Lifecycle Scope Architecture
