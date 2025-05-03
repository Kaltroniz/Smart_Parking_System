#include <Wire.h>

#define NUM_SLOTS 5
// LED pins for each slot mapped to D0, D3, D4, D5, D6
const int ledPins[NUM_SLOTS] = {D0, D3, D4, D5, D6};
const int buzzerPin = D7; // Buzzer pin

int currentSlot = -1;

void setup() {
  Serial.begin(115200);

  // Join I2C bus with address 0x08
  Wire.begin(0x08); 
  Wire.onReceive(receiveEvent); // Register I2C receive event

  // Set LED pins and buzzer pin as OUTPUT
  for (int i = 0; i < NUM_SLOTS; i++) {
    pinMode(ledPins[i], OUTPUT);
    digitalWrite(ledPins[i], LOW); // Initially turn off all LEDs
  }

  pinMode(buzzerPin, OUTPUT);  // Set buzzer pin as output
  digitalWrite(buzzerPin, LOW); // Initially turn off buzzer

  Serial.println("🔌 Slave ready.");
}

void loop() {
  // You can extend this logic to automatically turn off LEDs after a timeout or when a car is detected.
  // For now, the LED stays on for the slot as long as the car isn't detected.

  // Example: If a timeout or parking status condition occurs, turn off the LED and buzzer
  if (currentSlot != -1) {
    delay(10000); // 10 seconds delay for demo purposes
    digitalWrite(ledPins[currentSlot], LOW); // Turn off the LED after a delay
    digitalWrite(buzzerPin, LOW);  // Turn off the buzzer after 10 seconds
    Serial.print("🔴 Slot LED OFF for slot ");
    Serial.println(currentSlot);
    currentSlot = -1; // Reset current slot
  }
}

// This function is triggered when the master sends data via I2C
void receiveEvent(int howMany) {
  if (howMany >= 1) {
    int slot = Wire.read(); // Read the slot index sent by master
    if (slot >= 0 && slot < NUM_SLOTS) {
      // Turn off all LEDs
      for (int i = 0; i < NUM_SLOTS; i++) {
        digitalWrite(ledPins[i], LOW);
      }
      // Turn on LED for selected slot
      digitalWrite(ledPins[slot], HIGH);
      // Turn on the buzzer for 1 second
      digitalWrite(buzzerPin, HIGH);
      delay(1000); // Buzzer on for 1 second
      digitalWrite(buzzerPin, LOW);
      
      currentSlot = slot; // Update current slot
      Serial.print("💡 Slot LED ON for slot ");
      Serial.println(slot);
    }
  }
}
