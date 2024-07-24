/*!
 * @file    main.cpp
 * @brief   Implement the main logic of the smart bin
 * @author  [Tanaka Chitete](tchi8439@uni.sydney.edu.au)
 * @version 2.3
 * @date    05/10/2023
 * @link    https://github.sydney.edu.au/tchi8439/assignment
 */

#include <Arduino.h>
// #include <BLEDevice.h>
// #include <BLEServer.h>
// #include <BLEUtils.h>
// #include <BLE2902.h>
#include <DFRobot_HX711_I2C.h>
#include <Reading.h>

#include <cmath>
#include <iomanip>
#include <list>
#include <sstream>
#include <string>
#include <Arduino.h>
#include <WiFi.h>
#include <MQTT.h>


#define CALIBRATION_VALUE           1638.45f
#define READING_TOLERANCE_WEIGHT    10.0f
#define DELAY_TIME                  1000
#define SERVICE_UUID                "DFCD0001-36E1-4688-B7F5-EA07361B26A8"
#define CHARACTERISTIC_UUID         "DFCD000A-36E1-4688-B7F5-EA07361B26A8"

// WiFi setting
const char ssid[] = "Siddek";
const char pass[] = "abc12345678";
// MQTT settings
IPAddress mqttHost(172, 20, 10, 8);  // make sure you change this IP to your MQTT host (ie. your computer)

WiFiClient wifiNet;
MQTTClient mqttClient;

unsigned long lastTime = 0;

// this is function prototype
void messageReceived(String &topic, String &payload); // declaration at the bottom of the code
void calibrateScale();
// void setupBluetooth();
void setupMqtt();
void setupWIFI();
float calculateTotal(std::chrono::system_clock::time_point endTime);
std::string toString(float number);
char msg_out_weight[20];
char msg_out_accumulate[50];

DFRobot_HX711_I2C scale;
std::list<Reading> readings = {0.0f};

// BLEServer* server;
// BLEService* service;
// BLECharacteristic* characteristic;

// class MyServerCallbacks: public BLEServerCallbacks {
//     void onConnect(BLEServer* server) { }
//     void onDisconnect(BLEServer* server) { }
// };

/* Adapted from DFRobot (https://wiki.dfrobot.com/FireBeetle_Board_ESP32_E_SKU_DFR0654#target_24) */
// class MyCallbacks: public BLECharacteristicCallbacks {
//     void onWrite(BLECharacteristic* characteristic) { 
//         std::string request = characteristic->getValue();
        
//         // If the user requests the total amount of waste from the past 24 hours
//         if (request == "day") {
//             std::chrono::system_clock::time_point startTime = std::chrono::system_clock::now();
//             std::chrono::system_clock::time_point endTime = startTime - std::chrono::hours(1 * 24);
//             float totalWaste = calculateTotal(endTime);
//             characteristic->setValue("Total amount of waste today is " + toString(totalWaste) + 
//                 "g");
//             characteristic->notify();
//         // If the user requests the total amount of waste from the past 7 days
//         } else if (request == "week") {
//             std::chrono::system_clock::time_point startTime = std::chrono::system_clock::now();
//             std::chrono::system_clock::time_point endTime = startTime - std::chrono::hours(7 * 24);
//             float totalWaste = calculateTotal(endTime);
//             characteristic->setValue("Total amount of waste this week is " + toString(totalWaste) + 
//                 "g");
//             characteristic->notify();
//         // If the user requests the total amount of waste from the past month
//         } else if (request == "month") {
//             std::chrono::system_clock::time_point startTime = std::chrono::system_clock::now();
//             std::chrono::system_clock::time_point endTime = startTime - std::chrono::hours(28 * 24);
//             float totalWaste = calculateTotal(endTime);
//             characteristic->setValue("Total amount of waste this month is " + toString(totalWaste) + 
//                 "g");
//             characteristic->notify();
//         // If the user requests to reset the bin (i.e. delete the logged readings)
//         } else if (request == "reset") {
//             readings = {0.0f};
//             characteristic->setValue("Bin has been reset");
//             characteristic->notify();
//         }
//     }
// };

void setup() {
    Serial.begin(115200);
    setupWIFI();
    setupMqtt();
    calibrateScale();
    // setupBluetooth();
    delay(DELAY_TIME);
}

/* Adapted from DFRobot (https://wiki.dfrobot.com/HX711_Weight_Sensor_Kit_SKU_KIT0176) */
void calibrateScale() {
    // Attempt to start the scale
    while (!scale.begin()) {
        Serial.println("Failed to start bin");
        delay(DELAY_TIME);
    }
    // Calibrate the scale
    scale.setCalibration(CALIBRATION_VALUE);
}
void setupWIFI(){
    Serial.print("Connecting to WiFi");
    Serial.println(ssid);
    WiFi.begin(ssid, pass);

    // Connecting to WiFi
    int notConnectedCounter = 0;
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(" . ");
        notConnectedCounter++;
        if(notConnectedCounter > 20) { // Reset board if not connected after 5s
            Serial.println("Resetting due to Wifi not connecting (try restarting your hotspot)...");
            ESP.restart();
        }
    }
    Serial.print("\nWifi connected, IP address: ");
    Serial.println(WiFi.localIP());

}
/*Adapted from awit8837 Anusha Withana (https://github.sydney.edu.au/AID-LAB/Tutorials/blob/main/ESP32-MQTT/main.cpp)*/
void setupMqtt(){
    // Connecting to MQTT
    mqttClient.begin(mqttHost, wifiNet);
    mqttClient.onMessage(messageReceived); // this function will be called when a message is received from MQTT

    Serial.print("Connecting to MQTT host IP:");
    Serial.println(mqttHost);
    while (!mqttClient.connect("Firebeetle_connect", "firebeetle", "1234")) { // this string will be your client ID, you can change it
        Serial.print(" . ");
        delay(1000);
    }
    Serial.println("\nMQTT connected!\n");

    // Subscribe to topics
    mqttClient.subscribe("/control");
    // mqttClient.subscribe("/sensors/temp/date");
}

/* Adapted from DFRobot (https://wiki.dfrobot.com/FireBeetle_Board_ESP32_E_SKU_DFR0654#target_24) */
// void setupBluetooth() {
//     // Create device
//     BLEDevice::init("SmartBin");
//     // Create server
//     server = BLEDevice::createServer();
//     // Set the server's callback functions
//     server->setCallbacks(new MyServerCallbacks());
//     // Create service
//     service = server->createService(SERVICE_UUID); 
//     // Create a characteristic for the service
//     characteristic = service->createCharacteristic(
//         CHARACTERISTIC_UUID,
//         BLECharacteristic::PROPERTY_READ |
//         BLECharacteristic::PROPERTY_NOTIFY |
//         BLECharacteristic::PROPERTY_WRITE);
//     // Set the characteristic's callback functions
//     characteristic->setCallbacks(new MyCallbacks());
//     // Set a descriptor for this characteristic
//     characteristic->addDescriptor(new BLE2902());
//     // Start the service
//     service->start();
//     // Advertise the server
//     BLEAdvertising *advertising = server->getAdvertising();
//     advertising->start();
// }

void loop() {

    mqttClient.loop(); // loop the client
    // Retrieve new reading
    float reading = scale.readWeight();
    if (reading < 1.0) {
        reading = 0.0;
    }

    mqttClient.publish("/accumulate", msg_out_accumulate);
    // If the new reading is different from the previous (the amount of waste in the bin changed)
    if (std::fabs(reading - readings.back().getWeight()) > READING_TOLERANCE_WEIGHT) {
        // Log the new waste
        readings.push_back(Reading(reading));

        // Print the now-current waste
        Serial.print("Current amount of waste is ");
        Serial.print(readings.back().getWeight(), 0);
        Serial.println("g");
        sprintf(msg_out_weight, "%.1f", readings.back().getWeight());
        mqttClient.publish("/weight", msg_out_weight);

        // // Send a notification about the now-current waste
        // characteristic->setValue("Current amount of waste is " + 
        //     toString(readings.back().getWeight()) + "g");
        // characteristic->notify();

        // If the bin is full
        if (readings.back().getWeight() > 300.0) {
            Serial.println("Bin is now full");
            // characteristic->setValue("Bin is now full");
            // characteristic->notify();
            sprintf(msg_out_accumulate, "Bin is now full");
            mqttClient.publish("/accumulate", msg_out_accumulate);
        }
    }

    delay(DELAY_TIME);
}

std::string toString(float number) {
    std::stringstream stringStream;
    stringStream << std::fixed << std::setprecision(0) << number;
    std::string string = stringStream.str();
    stringStream.str(std::string());
    stringStream.clear();

    return string;
}

float calculateTotal(std::chrono::system_clock::time_point endTime) {
    auto previousIterator = readings.begin(); // Iterator for the first reading
    auto currentIterator = std::next(previousIterator); // Iterator for the second reading
    float total = 0.0f;
    while (currentIterator != readings.end() && currentIterator->getTime() >= endTime) {
        float change = currentIterator->getWeight() - previousIterator->getWeight();
        if (change > 0.0f) {
            total += change;
        }

        ++previousIterator;
        ++currentIterator;
    }
    return total;
}

void messageReceived(String &topic, String &payload) {
    Serial.println("incoming: " + topic + " - " + payload); // print the payload
    if (payload == "day") {
        std::chrono::system_clock::time_point startTime = std::chrono::system_clock::now();
        std::chrono::system_clock::time_point endTime = startTime - std::chrono::hours(1 * 24);
        float totalWaste = calculateTotal(endTime);
        sprintf(msg_out_accumulate, "Total amount of waste  today is %.2f g", totalWaste);

    // If the user requests the total amount of waste from the past 7 days
    } else if (payload == "week") {
        std::chrono::system_clock::time_point startTime = std::chrono::system_clock::now();
        std::chrono::system_clock::time_point endTime = startTime - std::chrono::hours(7 * 24);
        float totalWaste = calculateTotal(endTime);
        sprintf(msg_out_accumulate, "Total amount of waste this week is %.2f g", totalWaste);
      
    // If the user requests the total amount of waste from the past month
    } else if (payload == "month") {
        std::chrono::system_clock::time_point startTime = std::chrono::system_clock::now();
        std::chrono::system_clock::time_point endTime = startTime - std::chrono::hours(28 * 24);
        float totalWaste = calculateTotal(endTime);
        sprintf(msg_out_accumulate, "Total amount of waste this month is %.2f g", totalWaste);
        
    // If the user requests to reset the bin (i.e. delete the logged readings)
    } else if (payload == "reset") {
        readings = {0.0f};
        sprintf(msg_out_accumulate, "Bin has been reset");
    }

    // Note: Do not use the mqttClient in the callback to publish, subscribe or
    // unsubscribe as it may cause deadlocks when other things arrive while
    // sending and receiving acknowledgments. Instead, change a global variable,
    // or push to a queue and handle it in the loop after calling `mqttClient.loop()`.
}
