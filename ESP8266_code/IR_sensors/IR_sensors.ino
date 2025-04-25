// IR sensor pins
const int sensorPins[6] = {D1, D2, D3, D4, D5, D6}; // NodeMCU pin labels
bool sensorStates[6];

void setup() {
  Serial.begin(9600);
  
  // Initialize sensor pins
  for (int i = 0; i < 6; i++) {
    pinMode(sensorPins[i], INPUT);
  }

  Serial.println("Parking System Initialized.");
}

void loop() {
  for (int i = 0; i < 6; i++) {
    sensorStates[i] = digitalRead(sensorPins[i]);

    if (sensorStates[i] == LOW) { // Assuming LOW = Obstacle detected (car present)
      Serial.print("Slot ");
      Serial.print(i + 1);
      Serial.println(": Occupied");
    } else {
      Serial.print("Slot ");
      Serial.print(i + 1);
      Serial.println(": Free");
    }
  }

  Serial.println("-----");
  delay(2000); // Check every 2 seconds
}
