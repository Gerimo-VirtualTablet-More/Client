package com.antozstudios.drawnow;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.antozstudios.drawnow.Helper.HelperMethods;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import AI.Abc;

public class MenuActivity extends AppCompatActivity {

    DatagramSocket datagramSocket;
    Thread broadcastThread;
    Thread listenThread;
    private volatile boolean isRunning = true;
    private final Set<String> foundServers = new HashSet<>();


    int screenWidth,screenHeight;

    int hostWidth,hostHeight;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        new Abc(this);

        Thread initThread = new Thread(() -> {
            try {
                datagramSocket = new DatagramSocket(5000);
                datagramSocket.setBroadcast(true);
                startBroadcastThread();
                startListenThread();
            } catch (SocketException e) {
                Log.e("MenuActivity", "SocketException: " + e.getMessage());
                isRunning = false;
            }
        });
        initThread.start();

        HelperMethods.setUI(this);
    }

    private void startBroadcastThread() {
        broadcastThread = new Thread(() -> {
            try {
                String msg = "Ping";
                byte[] sendData = msg.getBytes();
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddress, 5000);

                while (isRunning) {
                    try {
                        if(datagramSocket != null && !datagramSocket.isClosed()) {
                            datagramSocket.send(sendPacket);
                        }
                        Thread.sleep(2000);
                    } catch (IOException e) {
                        if (isRunning) {
                            Log.e("MenuActivity", "IOException beim Senden: " + e.getMessage());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    Log.e("MenuActivity", "Fehler im Broadcast-Thread: " + e.getMessage());
                }
            }
        });
        broadcastThread.start();
    }

    private void startListenThread() {
        listenThread = new Thread(() -> {
            try {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                while (isRunning) {
                    try {
                        if(datagramSocket != null && !datagramSocket.isClosed()){
                            datagramSocket.receive(receivePacket);


                            String senderIp = receivePacket.getAddress().getHostAddress();


                            String myIp = getIpAddress();

                            if (senderIp != null && !senderIp.equals(myIp)) {
                                String receivedMsg = new String(receivePacket.getData(), 0, receivePacket.getLength());
                                String[] splitMsg = receivedMsg.split(";");
                                // Fügen Sie den Server nur hinzu, wenn er neu ist.
                                if (foundServers.add(senderIp)) {
                                    Log.d("Erhalten", "Server gefunden: " + receivedMsg + " von " + senderIp);
                                    // UI-Update auf dem Haupt-Thread ausführen
                                    hostWidth = Integer.parseInt(splitMsg[1]);
                                    hostHeight = Integer.parseInt(splitMsg[2]);

                                    runOnUiThread(() -> addServerButton(senderIp));
                                }
                            }
                        }
                    } catch (IOException e) {
                        if (isRunning) {
                            Log.e("MenuActivity", "IOException beim Empfangen: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    Log.e("MenuActivity", "Fehler im Listen-Thread: " + e.getMessage());
                }
            }
        });
        listenThread.start();
    }

    private void addServerButton(String serverIp) {
        Button button = new Button(this);
        button.setText(serverIp);

        LinearLayout layout = findViewById(R.id.linearlayout_scrollview);
        layout.addView(button);

        button.setOnClickListener((view) -> {
            // Stoppen Sie die Netzwerk-Threads, bevor Sie die Activity wechseln
            isRunning = false;

            // Erstellen des Intents und übergeben der IP als Extra
            Intent intent = new Intent(MenuActivity.this, DrawActivity.class);
            intent.putExtra(DrawActivity.EXTRA_SERVER_IP, serverIp);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;

        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
        }

        if (broadcastThread != null) {
            broadcastThread.interrupt();
        }
        if (listenThread != null) {
            listenThread.interrupt();
        }
    }

    private String getIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifiManager != null) {
                int ip = wifiManager.getConnectionInfo().getIpAddress();
                return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
            }
        } catch (Exception e) {
            Log.e("MenuActivity", "IP-Adresse konnte nicht abgerufen werden: " + e.getMessage());
        }
        return null;
    }
}
