#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>

// Wi-Fi
#define WIFI_SSID     "IITRPR"
#define WIFI_PASSWORD "V#6qF?pyM!bQ$%NX"

// Firebase
#define FIREBASE_HOST "smart-parking-system-114b2-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "6jnv4B2Nu2uoaA1gxpA21fi9vYs5vBxdsf1wA3by"

// Hardware
const int ledPins[] = { D0, D3, D4, D5, D6 };
const int buzzerPin = D7;
const int NUM_SLOTS = 5;

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// State
bool lastOpenState = false;
int  activeSlot    = -1;

void connectWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) delay(500);
}

void setup() {
  Serial.begin(115200);
  for (int i = 0; i < NUM_SLOTS; i++) {
    pinMode(ledPins[i], OUTPUT);
    digitalWrite(ledPins[i], LOW);
  }
  pinMode(buzzerPin, OUTPUT);
  digitalWrite(buzzerPin, LOW);

  connectWiFi();
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) connectWiFi();
  if (!Firebase.ready()) { delay(200); return; }

  // 1) Sample /gateControl/open
  if (Firebase.getBool(fbdo, "/gateControl/open")) {
    bool currOpen = fbdo.boolData();

    // rising edge => new booking
    if (currOpen && !lastOpenState) {
      if (Firebase.getInt(fbdo, "/gateControl/slotIndex")) {
        int slot = fbdo.intData();
        if (slot >= 0 && slot < NUM_SLOTS) {
          activeSlot = slot;
          // LED on + beep
          digitalWrite(ledPins[slot], HIGH);
          digitalWrite(buzzerPin, HIGH);
          delay(200);
          digitalWrite(buzzerPin, LOW);
        }
      }
    }
    lastOpenState = currOpen;
  }

  // 2) If we have an activeSlot, watch its occupancy
  if (activeSlot >= 0) {
    String path = "/parking_slots/" + String(activeSlot);
    if (Firebase.getString(fbdo, path.c_str())) {
      if (fbdo.stringData() == "occupied") {
        // turn LED off once—and never blink again
        digitalWrite(ledPins[activeSlot], LOW);
        activeSlot = -1;
      }
    }
  }

  delay(200);
}
