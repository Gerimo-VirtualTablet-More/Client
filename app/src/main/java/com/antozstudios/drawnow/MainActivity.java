package com.antozstudios.drawnow;

import static android.view.MotionEvent.AXIS_X;
import static android.view.MotionEvent.TOOL_TYPE_STYLUS;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.antozstudios.drawnow.databinding.ActivityMainBinding;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "UDPClient";
    private static final String SERVER_IP = "192.168.0.105"; // anpassen
    private static final int SERVER_PORT = 5000;

    private ActivityMainBinding binding;

    private DatagramSocket udpSocket;
    private InetAddress serverAddress;

    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
    private Thread senderThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
        ).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        initUDP();
    }

    private void initUDP() {
        try {
            udpSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_IP);

            senderThread = new Thread(this::sendLoop);
            senderThread.start();

            Log.i(TAG, "UDP-Client gestartet. Ziel: " + SERVER_IP + ":" + SERVER_PORT);

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Initialisieren von UDP: ", e);
        }
    }

    private void sendLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String msg = sendQueue.take();
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
                udpSocket.send(packet);
            }
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Senden: ", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeUDP();
    }

    private void closeUDP() {
        if (senderThread != null) senderThread.interrupt();
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        Log.i(TAG, "UDP-Client geschlossen");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getToolType(0) == TOOL_TYPE_STYLUS) {

                int x = (int) event.getX();
                int y = (int) event.getY();
                float pressure = event.getPressure();

                sendQueue.offer(x + ";" + y + ";" + pressure);

            }

        return super.onTouchEvent(event);
    }
}
