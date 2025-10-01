#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <Adafruit_PWMServoDriver.h>
#include <ArduinoJson.h>

Adafruit_PWMServoDriver board1 = Adafruit_PWMServoDriver(0x40); //default address 0x40   
#define SERVOMIN  125      // minimum pulse length count (out of 4096)
#define SERVOMAX  625      // maximum pulse length count (out of 4096)
#define MinArmAng 40
#define MaxArmAng 140
#define MinGrapAng 120
#define MaxGrapAng 165

// Update these with values suitable for your network.
const char* ssid = "I got Kiev before Putin A53";
const char* passwordID = "audamland18";
const char* mqtt_server = "221bc06addf44c7c8801b7ea585ff297.s1.eu.hivemq.cloud";
const char* username = "esp8266";
const char* password = "Haitc007";
int base_angle = 90;
int shoulder_angle = 90;
int elbow_angle = 90;
int wriste_angle = 90;
int wriste_rot_angle = 90;
int gripper_angle = 120;

WiFiClientSecure espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
#define MSG_BUFFER_SIZE (500)
char msg[MSG_BUFFER_SIZE];
int value = 0;

void setup_wifi() {
  delay(10);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, passwordID);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  randomSeed(micros());
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  String message = "";
  for (int i = 0; i < length; i++) {
    //Serial.print((char)payload[i]);
    message += (char)payload[i];
  }
  Serial.println(message);
  Serial.println();
  JsonDocument doc;
  deserializeJson(doc, message);

  // Fetch values.
  //
  // Most of the time, you can rely on the implicit casts.
  // In other case, you can do doc["time"].as<long>();
  int servo1 = doc["servo1"];
  int servo2 = doc["servo2"];
  int servo3 = doc["servo3"];
  int servo4 = doc["servo4"];
  int servo5 = doc["servo5"];
  int servo6 = doc["servo6"];
  
  Write_Servo(servo1, 0);
  Write_Servo(servo2, 2);
  Write_Servo(servo3, 4);
  Write_Servo(servo4, 6);
  Write_Servo(servo5, 8);
  Write_Servo(servo6, 10);
}


void reconnect() {
  // Loop until we’re reconnected
  while (!client.connected()) {
    Serial.println("Attempting MQTT connection…");
    String clientId = "ESP8266Client";
    // Attempt to connect
    // Insert username and password
    if (client.connect(clientId.c_str(), username, password)) {
      Serial.println("connected");
      // publish robot current pos
      //Creating a json
      char buffer[256];
      JsonDocument message;
      message["servo1"] = base_angle;
      message["servo2"] = shoulder_angle;
      message["servo3"] = elbow_angle;
      message["servo4"] = wriste_angle;
      message["servo5"] = wriste_rot_angle;
      message["servo6"] = gripper_angle;
      serializeJson(message, buffer);
      // Publish an MQTT message on topic value
      client.publish("value", buffer);
      // … and resubscribe
      client.subscribe("value");
    } else {
      Serial.print("failed, rc = ");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

int angleToPulse(int ang)  //gets angle in degree and returns the pulse width
{ 
  int pulse = map(ang,0, 180, SERVOMIN,SERVOMAX);  // map angle of 0 to 180 to Servo min and Servo max 
  return pulse;
}

void Write_Servo(int servo_angle, int servoPIN)
{
  board1.setPWM(servoPIN, 0, angleToPulse(servo_angle));
  Serial.print("Servo: ");
  Serial.println(servoPIN);
  Serial.print("Angle: ");
  Serial.println(servo_angle);
  delay(500);
} 

void setup() {
  Serial.begin(9600);
  board1.begin();
  board1.setPWMFreq(60);    // Analog servos run at ~60 Hz updates
  //setup robot starting position
  board1.setPWM(0, 0, angleToPulse(90));
  delay(500);
  board1.setPWM(2, 0, angleToPulse(90));
  delay(500);
  board1.setPWM(4, 0, angleToPulse(90));
  delay(500);
  board1.setPWM(6, 0, angleToPulse(90));
  delay(500);
  board1.setPWM(8, 0, angleToPulse(90));
  delay(500);
  board1.setPWM(10, 0, angleToPulse(120));
  delay(500);
  //Connect wifi
  setup_wifi();
  // you can use the insecure mode, when you want to avoid the certificates
  espClient.setInsecure();
  client.setServer(mqtt_server, 8883);
  client.setCallback(callback);  
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
}