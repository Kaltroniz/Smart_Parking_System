#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>
#include <Servo.h>

// Wi-Fi credentials
#define WIFI_SSID     "IITRPR"
#define WIFI_PASSWORD "V#6qF?pyM!bQ$%NX"

// Firebase credentials
#define FIREBASE_HOST "smart-parking-system-114b2-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "6jnv4B2Nu2uoaA1gxpA21fi9vYs5vBxdsf1wA3by"

const int irPins[5] = { D1, D2, D5, D6, D3 };// IR sensor pins (use only pins with pull-up support)

// Gate servo
const int servoPin = D7;
Servo     gateServo;

// Firebase objects
FirebaseData   fbdo;
FirebaseAuth   auth;// for authentication in firebase
FirebaseConfig config;

bool lastReqOpen = false;// Track last open-request state for rising-edge detection

void connectToWiFi() {
  Serial.print("🔌 Connecting to WiFi");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" ✅");
}

void setup() {
  Serial.begin(115200);// for begin the data
  while (!Serial) {}

  // Initialize IR inputs with pull-ups
  //here we use 5 IR sensors
  for (int i = 0; i < 5; i++) {
    pinMode(irPins[i], INPUT_PULLUP);
  }

  // Initialize servo
  gateServo.attach(servoPin);
  gateServo.write(0);  // start closed

  connectToWiFi();// for Connecting to the Wi-Fi

  // Initialize Firebase
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  Serial.println(Firebase.ready() ? "✅ Firebase connected" 
                                 : "❌ Firebase init failed");
}

void loop() {
  // Keep Wi-Fi alive
  if (WiFi.status() != WL_CONNECTED) {
    connectToWiFi();
  }
  if (!Firebase.ready()) {
    delay(200);
    return;
  }

  // 1) Read all sensors, print & push to Firebase
  Serial.println("---- Slot Statuses ----");
  for (int i = 0; i < 5; i++) {
    bool occupied = (digitalRead(irPins[i]) == LOW);
    const char* status = occupied ? "occupied" : "available";
    Serial.printf("Slot %d: %s\n", i, status);
    Firebase.setString(fbdo,
      String("/parking_slots/") + i,
      status
    );
  }
  Serial.println("-----------------------");

  // 2) Handle gate-open requests on the rising edge
  if (Firebase.getBool(fbdo, "/gateControl/open")) {
    bool reqOpen = fbdo.boolData();
    if (reqOpen && !lastReqOpen) {
      // clear the flag immediately
      Firebase.setBool(fbdo, "/gateControl/open", false);

      // fetch slotIndex
      if (Firebase.getInt(fbdo, "/gateControl/slotIndex")) {
        int slot = fbdo.intData();
        String path = "/parking_slots/" + String(slot);

        // only open if still available
        if (slot >= 0 && slot < 5
            && Firebase.getString(fbdo, path.c_str())
            && fbdo.stringData() == "available") {
          Serial.printf("🟢 Opening gate for slot %d\n", slot);
          gateServo.write(150);
          delay(10000);
          Serial.println("🔴 Closing gate");
          gateServo.write(0);
        }
      }
    }
    lastReqOpen = reqOpen;
  } else {
    lastReqOpen = false;
  }

  delay(500);
}
