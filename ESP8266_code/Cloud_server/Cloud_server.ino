#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <Servo.h>
#include <Wire.h>

// WiFi credentials
#define WIFI_SSID "IITRPR"
#define WIFI_PASSWORD "V#6qF?pyM!bQ$%NX"

// Firebase credentials
#define FIREBASE_HOST "smart-parking-system-114b2-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "6jnv4B2Nu2uoaA1gxpA21fi9fYs5vBxdsf1wA3by"

// Firebase objects
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// IR sensor pins (5 sensors)
const int irPins[5] = {D0, D3, D4, D5, D6};

// Gate control
Servo gateServo;
const int servoPin = D7;
bool gateOpen = false;

void connectToWiFi() {
  Serial.print("🔌 Connecting to WiFi");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\n✅ WiFi connected!");
}

void setup() {
  Serial.begin(115200);
  Wire.begin(); // I2C Master

  // Set IR sensor pins
  for (int i = 0; i < 5; i++) {
    pinMode(irPins[i], INPUT);
  }

  // Set up gate servo
  gateServo.attach(servoPin);
  gateServo.write(0); // Closed

  connectToWiFi();

  // Configure Firebase
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Test Firebase connection
  if (Firebase.ready()) {
    Serial.println("✅ Firebase connected.");
  } else {
    Serial.println("❌ Firebase not ready.");
  }
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectToWiFi();
  }

  if (!Firebase.ready()) {
    Serial.println("⚠ Firebase not ready yet...");
    delay(1000);
    return;
  }

  // 1. IR SENSOR UPDATES
  for (int i = 0; i < 5; i++) {
    int state = digitalRead(irPins[i]); // 0 = object detected
    String path = "/parking_slots/" + String(i);
    String status = (state == 0) ? "occupied" : "available";

    if (Firebase.setString(fbdo, path.c_str(), status)) {
      Serial.print("✅ Slot ");
      Serial.print(i);
      Serial.print(": ");
      Serial.println(status);
    } else {
      Serial.print("❌ Slot ");
      Serial.print(i);
      Serial.print(" update failed: ");
      Serial.println(fbdo.errorReason());
    }
  }

  // 2. GATE CONTROL FROM FIREBASE
  if (Firebase.getBool(fbdo, "/gateControl/open")) {
    bool gateControl = fbdo.boolData();

    if (gateControl && !gateOpen) {
      Serial.println("🟢 gateControl = true -> Opening gate...");
      gateServo.write(150); // Open
      gateOpen = true;

      // Send booked slot index to I2C slave
      if (Firebase.getInt(fbdo, "/gateControl/slotIndex")) {
        int bookedSlot = fbdo.intData();
        if (bookedSlot >= 0 && bookedSlot < 5) {
          Serial.print("📨 Sending booked slot to slave: ");
          Serial.println(bookedSlot);
          Wire.beginTransmission(0x08);
          Wire.write(bookedSlot); // Send slot index to slave
          Wire.endTransmission();
        } else {
          Serial.println("❌ Invalid slot index received.");
        }
      } else {
        Serial.print("❌ Failed to get booked slot index: ");
        Serial.println(fbdo.errorReason());
      }

      // Wait for 10 seconds before closing
      delay(10000);
      Serial.println("🔴 Closing gate...");
      gateServo.write(0); // Close
      gateOpen = false;

      // Reset gate control
      if (Firebase.setBool(fbdo, "/gateControl/open", false)) {
        Serial.println("✅ gateControl set to false.");
      } else {
        Serial.print("❌ Failed to reset gateControl: ");
        Serial.println(fbdo.errorReason());
      }
    }
  } else {
    Serial.print("❌ Failed to read /gateControl/open: ");
    Serial.println(fbdo.errorReason());
  }

  Serial.println("--------------------------------");
  delay(2000);
}
