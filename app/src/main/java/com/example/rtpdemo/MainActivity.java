package com.example.rtpdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("jrtplib");
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private String destIP;
    private int destPort;
    private MediaSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 123);
        session = new MediaSession();

        final Button btn = (Button) findViewById(R.id.btnCtrl);
        btn.setOnClickListener(new View.OnClickListener() {
            boolean isStarted = false;
            public void onClick(View view) {
                if(!isStarted) {
                    EditText textIP = (EditText) findViewById(R.id.txtIP);
                    EditText textPort = (EditText) findViewById(R.id.txtPort);
                    destIP = textIP.getText().toString();
                    destPort = Integer.parseInt(textPort.getText().toString());
                    session.start(destIP, destPort);
                    btn.setText("Stop");
                    isStarted = true;
                }
                else {
                    session.stop();
                    btn.setText("Start");
                    isStarted = false;
                }
            }
        });


    }



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */


}
