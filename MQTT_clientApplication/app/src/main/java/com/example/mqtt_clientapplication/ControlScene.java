package com.example.mqtt_clientapplication;

import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;

import java.util.NoSuchElementException;

public class ControlScene extends AppCompatActivity {

    final String host = "221bc06addf44c7c8801b7ea585ff297.s1.eu.hivemq.cloud";
    final int port = 8883;
    String username = "";
    String password = "";
    final int MinArmAng = 20;
    final int MaxArmAng = 160;
    final int MinGripAng = 120;
    final int MaxGripAng = 165;
    Mqtt5BlockingClient client;
    SeekBar servo1, servo2, servo3, servo4, servo5, servo6;
    Button btnDisconnect, btnSave_Pose, btnRun, btnReset;
    TextView angle1, angle2, angle3, angle4, angle5, angle6;
    int saved_pos = 0, index_pos = 0;
    int[] saved_values1 = new int[6];
    int[] saved_values2 = new int[6];
    int[] saved_values3 = new int[6];
    int[] saved_values4 = new int[6];
    int[] saved_values5 = new int[6];
    int[] saved_values6 = new int[6];
    boolean pause = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_scene);

        //Get the username and password from the LoginScene
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");

        // TextViews for servo angles
        angle1 = findViewById(R.id.angularBase);
        angle2 = findViewById(R.id.angularSholder);
        angle3 = findViewById(R.id.angularElbow);
        angle4 = findViewById(R.id.angularWriste);
        angle5 = findViewById(R.id.angularWristeRot);
        angle6 = findViewById(R.id.angularGriper);

        // SeekBars for servo values
        servo1 = findViewById(R.id.sbBase);
        servo2 = findViewById(R.id.sbSholder);
        servo3 = findViewById(R.id.sbElbow);
        servo4 = findViewById(R.id.sbWriste);
        servo5 = findViewById(R.id.sbWristeRot);
        servo6 = findViewById(R.id.sbGripper);

        Setup_seekbar();
        btnSave_Pose = findViewById(R.id.buttonSave);
        btnSave_Pose.setText("Save Pose 1");
        btnSave_Pose.setOnClickListener(v -> {
            btnRun.setEnabled(true);
            btnReset.setEnabled(true);
            // Save maximum 6 poses
            if (saved_pos < 6) {
                saved_values1[saved_pos] = servo1.getProgress();
                saved_values2[saved_pos] = servo2.getProgress();
                saved_values3[saved_pos] = servo3.getProgress();
                saved_values4[saved_pos] = servo4.getProgress();
                saved_values5[saved_pos] = servo5.getProgress();
                saved_values6[saved_pos] = servo6.getProgress();
                // 0 1 2 3 4 5
                saved_pos++;
                // 1 2 3 4 5 6
                Log.d("HAITC", "Saved pose " + saved_pos);

                // Update the button text
                if (saved_pos < 6)
                {
                    btnSave_Pose.setText("Save Pose " + (saved_pos + 1));
                }
                else
                {
                    btnSave_Pose.setText("Save Pose 6");
                    btnSave_Pose.setEnabled(false);
                }
            }
        });

        btnRun = findViewById(R.id.buttonStart);
        btnRun.setOnClickListener(v -> {
            if (btnRun.getText().toString().equals("Run"))
            {
                Run();
            }
            else
            {
                Pause();
            }
        });

        btnReset = findViewById(R.id.buttonResest);
        btnReset.setOnClickListener(v -> Reset());

        btnDisconnect = findViewById(R.id.buttonDisconnect);
        btnDisconnect.setOnClickListener(v -> {
            client.disconnect();
            finish();
        });

        // Create a new thread to connect to the MQTT broker
        connect();
    }

    void SetSeekBarListener(SeekBar seekBar) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                ControlScene.Servo_Value value = new ControlScene.Servo_Value();
                value.servo1 = servo1.getProgress();
                value.servo2 = servo2.getProgress();
                value.servo3 = servo3.getProgress();
                value.servo4 = servo4.getProgress();
                value.servo5 = servo5.getProgress();
                value.servo6 = servo6.getProgress();
                String json_message = new Gson().toJson(value);
                publish(json_message);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    void Run() {
        btnRun.setText("Pause");
        pause = false;
        btnSave_Pose.setEnabled(false);
        btnSave_Pose.setTextColor(getResources().getColor(R.color.gray));
        Disable_touch_seekbars();
        new Thread(() -> {
            while (!pause) {
                Update_progress(servo1, angle1, saved_values1[index_pos]);
                Update_progress(servo2, angle2, saved_values2[index_pos]);
                Update_progress(servo3, angle3, saved_values3[index_pos]);
                Update_progress(servo4, angle4, saved_values4[index_pos]);
                Update_progress(servo5, angle5, saved_values5[index_pos]);
                Update_progress(servo6, angle6, saved_values6[index_pos]);
                Log.d("HAITC", "Running pose " + (index_pos + 1) + " of " + saved_pos);
                ControlScene.Servo_Value value = new ControlScene.Servo_Value();
                value.servo1 = saved_values1[index_pos];
                value.servo2 = saved_values2[index_pos];
                value.servo3 = saved_values3[index_pos];
                value.servo4 = saved_values4[index_pos];
                value.servo5 = saved_values5[index_pos];
                value.servo6 = saved_values6[index_pos];
                String json_message = new Gson().toJson(value);
                publish(json_message);
                if (index_pos < saved_pos - 1) // 0 1 2 3 4 5
                {
                    index_pos++;
                } else // 6
                {
                    index_pos = 0;
                }
                //Delay 5 seconds per action
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    void Pause() {
        btnRun.setText("Run");
        pause = true;
        Enable_touch_seekbars();
        if (saved_pos < 6) // 0 1 2 3 4 5 - not reach max pose saved
        {
            btnSave_Pose.setEnabled(true);
        }
    }

    @SuppressLint("SetTextI18n")
    void Reset()
    {
        btnSave_Pose.setEnabled(true);
        btnSave_Pose.setText("Save Pose 1");
        saved_pos = 0;
        index_pos = 0;
        btnRun.setEnabled(false);
        btnRun.setTextColor(getResources().getColor(R.color.gray));
        btnRun.setText("Run");
        btnReset.setEnabled(false);
        btnReset.setTextColor(getResources().getColor(R.color.gray));
        pause = true;
        Enable_touch_seekbars();
        saved_values1 = new int[6];
        saved_values2 = new int[6];
        saved_values3 = new int[6];
        saved_values4 = new int[6];
        saved_values5 = new int[6];
        saved_values6 = new int[6];
        Set_default_pose();
        ControlScene.Servo_Value value = new ControlScene.Servo_Value();
        value.servo1 = 90;
        value.servo2 = 90;
        value.servo3 = 90;
        value.servo4 = 90;
        value.servo5 = 90;
        value.servo6 = 120;
        String json_message = new Gson().toJson(value);
        publish(json_message);
    }

    void Set_default_pose()
    {
        Update_progress(servo1, angle1, 90);
        Update_progress(servo2, angle2, 90);
        Update_progress(servo3, angle3, 90);
        Update_progress(servo4, angle4, 90);
        Update_progress(servo5, angle5, 90);
        Update_progress(servo6, angle6, 120);
    }

    void Disable_touch_seekbars()
    {
        servo1.setEnabled(false);
        servo2.setEnabled(false);
        servo3.setEnabled(false);
        servo4.setEnabled(false);
        servo5.setEnabled(false);
        servo6.setEnabled(false);
    }

    void Enable_touch_seekbars()
    {
        servo1.setEnabled(true);
        servo2.setEnabled(true);
        servo3.setEnabled(true);
        servo4.setEnabled(true);
        servo5.setEnabled(true);
        servo6.setEnabled(true);
    }

    public boolean connect() {
        // create an MQTT client
        client = MqttClient.builder()
                .useMqttVersion5()
                .serverHost(host)
                .serverPort(port)
                .sslWithDefaultConfig()
                .buildBlocking();
        Log.d("HAITC", "Client created");

        // connect to HiveMQ Cloud with TLS and username/password
        client.connectWith()
                .simpleAuth()
                .username(username)
                .password(UTF_8.encode(password))
                .applySimpleAuth()
                .send();
        Log.d("HAITC", "Connected successfully");

        // subscribe to the topic
        client.subscribeWith()
                .topicFilter("value")
                .send();
        Log.d("HAITC", "Subscribed to " + "value");

        // set a callback that is called when a message is received
        client.toAsync().publishes(ALL, publish -> {
            try {
                String message = UTF_8.decode(publish.getPayload().get()).toString();
                Receiving_data(message);
            } catch (NoSuchElementException e) {
                e.printStackTrace();
            }
        });
        return true;
    }

    @SuppressLint("SetTextI18n")
    void Update_progress(SeekBar seekBar, TextView textView, int progress)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                seekBar.setProgress(progress, true);
                textView.setText("" + progress);
            }
        });
    }

    // publish a message to the topic
    void publish(String message) {
        client.publishWith()
                .topic("value")
                .payload(UTF_8.encode(message))
                .send();
    }

    // deserialize the JSON message
    void Receiving_data(String message) {
        // convert the JSON message to a Java object
        ControlScene.Servo_Value json = new Gson().fromJson(message, ControlScene.Servo_Value.class);
        Log.d("HAITC","Received message, deserializing JSON: " + '\n'
                + "Servo 1: " + json.servo1 + '\n'
                + "Servo 2: " + json.servo2 + '\n'
                + "Servo 3: " + json.servo3 + '\n'
                + "Servo 4: " + json.servo4 + '\n'
                + "Servo 5: " + json.servo5 + '\n'
                + "Servo 6: " + json.servo6);

        //Updating in process bars
        Update_progress(servo1, angle1, json.servo1);
        Update_progress(servo2, angle2, json.servo2);
        Update_progress(servo3, angle3, json.servo3);
        Update_progress(servo4, angle4, json.servo4);
        Update_progress(servo5, angle5, json.servo5);
        Update_progress(servo6, angle6, json.servo6);
    }

    void Setup_seekbar()
    {
        Set_default_pose();
        //Disable_touch_seekbars();
        SetSeekBarListener(servo1);
        SetSeekBarListener(servo2);
        SetSeekBarListener(servo3);
        SetSeekBarListener(servo4);
        SetSeekBarListener(servo5);
        SetSeekBarListener(servo6);
        servo1.setMin(MinArmAng);
        servo1.setMax(MaxArmAng);
        servo2.setMin(MinArmAng);
        servo2.setMax(MaxArmAng);
        servo3.setMin(MinArmAng);
        servo3.setMax(MaxArmAng);
        servo4.setMin(MinArmAng);
        servo4.setMax(MaxArmAng);
        servo5.setMin(MinArmAng);
        servo5.setMax(MaxArmAng);
        servo6.setMin(120);
        servo6.setMax(MaxGripAng);
    }

    static class Servo_Value {
        public int servo1;
        public int servo2;
        public int servo3;
        public int servo4;
        public int servo5;
        public int servo6;
    }
}