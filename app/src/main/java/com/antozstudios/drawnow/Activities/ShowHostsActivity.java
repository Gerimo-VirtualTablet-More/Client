package com.antozstudios.drawnow.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.antozstudios.drawnow.databinding.ActivityShowHostsBinding;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ShowHostsActivity extends AppCompatActivity {

    ActivityShowHostsBinding binding;
    volatile boolean isSendingPing = true;
    DatagramSocket datagramSocket;
    InetAddress inetAddress;
    int port = 7000;
    Thread sendThread;
    Thread recieveThread;
    LinearLayout linearLayout;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShowHostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();

        sendThread = new Thread(() -> {
            String message = "Ping";
            DatagramPacket sendDataPacket = new DatagramPacket(message.getBytes(), message.length(), inetAddress, port);
            try {
                while (isSendingPing) {
                    try {
                        Thread.sleep(1000);
                        datagramSocket.send(sendDataPacket);
                    } catch (InterruptedException e) {
                        isSendingPing = false;
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        sendThread.start();

        recieveThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] buf = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    datagramSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (linearLayout != null) {
                        boolean contains = linearLayout.getTouchables()
                                .stream()
                                .map(view -> ((Button) view).getText())
                                .anyMatch(text -> text.equals(message));

                        if (!contains) {
                            runOnUiThread(() -> {
                                Button button = new Button(this);
                                button.setText(message);
                                button.setOnClickListener((view) -> {
                                    Intent intent = new Intent(ShowHostsActivity.this, DrawActivity.class)
                                            .putExtra("com.antozstudios.drawnow.SERVER_IP", message);
                                    startActivity(intent);
                                });
                                linearLayout.addView(button);
                            });
                        }
                    }
                    Log.d("Response", message);
                    Thread.sleep(1000);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        recieveThread.start();
    }

    void init() {
        linearLayout = binding.hostsLinearLayout;
        try {
            inetAddress = InetAddress.getByName("255.255.255.255");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(true);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sendThread != null) {
            sendThread.interrupt();
            isSendingPing = false;
        }
        if (recieveThread != null) {
            recieveThread.interrupt();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sendThread != null) {
            sendThread.interrupt();
            isSendingPing = false;
        }
        if (recieveThread != null) {
            recieveThread.interrupt();
        }
    }
}
