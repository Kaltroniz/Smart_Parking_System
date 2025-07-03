# 🚗 Smart Parking System

A modern Android app to simplify and automate the parking experience using Firebase and Jetpack Compose. The system helps users find, book, and access parking slots with real-time updates and seamless user experience.

## 🔧 Tech Stack

- **Kotlin** – Core language for Android development  
- **Jetpack Compose** – Modern UI toolkit for building responsive interfaces  
- **Firebase Authentication** – Google Sign-In support  
- **Firebase Realtime Database** – Live sync of parking slot availability and bookings  
- **ESP8266 + IR Sensors** – For detecting vehicle presence  
- **QR Code Scanner** – For secure gate access

## ✨ Features

- 🔐 **Authentication**  
  - Google Sign-In integration using Firebase Authentication  

- 🅿️ **Real-Time Slot Management**  
  - IR sensor data updates parking slot availability instantly  
  - Firebase Realtime Database for live sync  

- 📅 **Booking System**  
  - Users can book slots with a countdown timer  
  - Slots are auto-released if not occupied within a timeframe  
  - Billing interface (excluding payment gateway)  

- 🚪 **QR-Based Entry**  
  - Secure QR code scanning at the gate to validate parking  
  - Prevents unauthorized access  

- 📱 **Clean UI + Navigation**  
  - Built with Jetpack Compose  
  - Instagram-style bottom navigation using Jetpack Compose Navigation  
  - All UI and navigation logic in a single `MainActivity.kt`

---




## 📽️ Demo

🎥 [Watch the demo on YouTube](https://www.youtube.com/watch?v=cWispC73ozw)



## 🚀 Getting Started

### Prerequisites

- Android Studio (Latest version)
- Firebase Project (Enable Authentication and Realtime Database)
- ESP8266 with IR Sensors (for live slot status updates)

### Installation

1. Clone the repository  
   ```bash
   git clone https://github.com/your-username/smart-parking-system.git
   ```
2. Open in Android Studio

3. Add your Firebase config (google-services.json) to the app/ folder

4. Run the app on an emulator or physical device

