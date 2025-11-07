package com.antozstudios.drawnow.Activities;

import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.TOOL_TYPE_FINGER;
import static android.view.MotionEvent.TOOL_TYPE_STYLUS;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;


import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.antozstudios.drawnow.Helper.HelperMethods;
import com.antozstudios.drawnow.Helper.KeyHelper;
import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.Manager.ProfileManager;
import com.antozstudios.drawnow.databinding.ActivityDrawBinding;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DrawActivity extends AppCompatActivity {

    public static final String EXTRA_SERVER_IP = "com.antozstudios.drawnow.SERVER_IP";
    private static final String TAG = "UDPClient";

    private String SERVER_IP;
    private static final int SERVER_PORT = 5000;
    private ActivityDrawBinding binding;

    private DatagramSocket udpSocket;
    private InetAddress serverAddress;

    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
    private Thread senderThread;

    boolean lastTouchMode,touchMode, showLastPointMode;

    ViewPropertyAnimator anim1,anim2;

    ProfileManager profileManager;


    int paddingTopInPixels;


    PrefManager prefManager;
    private int clientWidth;
    private int clientHeight;
    private static final String KEY_TOUCH_MODE = "touchMode";
    private static final String KEY_SHOW_LAST_POINT_MODE = "showLastPointMode";
    private String activeMainProfileName; // Name of the main profile


    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDrawBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        
        
        paddingTopInPixels =  binding.drawActivity.getPaddingTop();

        binding.drawActivity.post(() -> {
            clientWidth = binding.drawActivity.getWidth();
            clientHeight = binding.drawActivity.getHeight();
        });

        prefManager = PrefManager.getInstance(this);
        profileManager = ProfileManager.getInstance(this);

         anim1= binding.pinchCursor1.animate();
         anim2= binding.pinchCursor2.animate();

        initDefaultFloatingButtons();


        
        


        binding.ToolButton.setOnClickListener(v -> {
            int currentVisibility = binding.leftToolbar.getVisibility();
            binding.leftToolbar.setVisibility(currentVisibility == VISIBLE ? GONE : VISIBLE);
        });


        binding.openShortcutButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ShortcutActivity.class));
        });
        binding.settingsButton.setOnClickListener(v ->{
           startActivity(new Intent(this, SettingsActivity.class));
        });



        String serverIpFromIntent = getIntent().getStringExtra(EXTRA_SERVER_IP);
        if (serverIpFromIntent != null && !serverIpFromIntent.isEmpty()) {
            SERVER_IP = serverIpFromIntent;
        } else {
            Log.e(TAG, "DrawActivity started without a server IP. Finishing activity.");
            finish();
            return;
        }

        HelperMethods.setUI(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        int toolPosition = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getInt(PrefManager.KeyPref.CURRENT_TOOLBAR_POSITION.getKey(), 0);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(binding.drawActivity);

        if (toolPosition == 0) {
            // Position on the left
            constraintSet.connect(binding.ToolButton.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            constraintSet.clear(binding.ToolButton.getId(), ConstraintSet.END);
        } else {
            // Position on the right
            constraintSet.connect(binding.ToolButton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            constraintSet.clear(binding.ToolButton.getId(), ConstraintSet.START);
        }

        constraintSet.applyTo(binding.drawActivity);

        initUDP();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_TOUCH_MODE, touchMode);
        outState.putBoolean(KEY_SHOW_LAST_POINT_MODE, showLastPointMode);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        touchMode = savedInstanceState.getBoolean(KEY_TOUCH_MODE);
        showLastPointMode = savedInstanceState.getBoolean(KEY_SHOW_LAST_POINT_MODE);
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

            if (showLastPointMode) {
                binding.cursorImage.setVisibility(VISIBLE);
                binding.cursorImage.setX(x - ((float) binding.cursorImage.getWidth() / 2));
                binding.cursorImage.setY(y - ((float) binding.cursorImage.getHeight() / 2));
                binding.cursorImage.setScaleX((float) Math.max(0.5, pressure));
                binding.cursorImage.setScaleY((float) Math.max(0.5, pressure));
            }

            if (clientWidth > 0 && clientHeight > 0) {
                float scaledX = x * (1920.0f / clientWidth);
                float scaledY = y * (1080.0f / clientHeight);
                sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.CLICK, (int) scaledX, (int) scaledY, pressure));
            } else {
                sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.CLICK, x, y, pressure));
            }
            hideToolBar(true);
        }

        if (touchMode) {
            handlePinchGesture(event);
        }
        
        return super.onTouchEvent(event);
    }

    private void handlePinchGesture(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            int action = event.getActionMasked();
            int pointerIndex1 = event.findPointerIndex(0);
            int pointerIndex2 = event.findPointerIndex(1);

            if (pointerIndex1 != -1 && pointerIndex2 != -1 &&
                event.getToolType(pointerIndex1) == TOOL_TYPE_FINGER &&
                event.getToolType(pointerIndex2) == TOOL_TYPE_FINGER) {

                int x1 = (int) event.getX(pointerIndex1);
                int y1 = (int) event.getY(pointerIndex1);
                int x2 = (int) event.getX(pointerIndex2);
                int y2 = (int) event.getY(pointerIndex2);

                switch (action) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        binding.pinchCursor1.setVisibility(VISIBLE);
                        binding.pinchCursor2.setVisibility(VISIBLE);
                        anim1.scaleX(1.5F).scaleY(1.5F).setDuration(100).start();
                        anim2.scaleX(1.5F).scaleY(1.5F).setDuration(100).start();
                        // Fall through to update position immediately
                    case MotionEvent.ACTION_MOVE:
                        binding.pinchCursor1.setX(x1 - ((float) binding.pinchCursor1.getWidth() / 2));
                        binding.pinchCursor1.setY(y1 - ((float) binding.pinchCursor1.getHeight() / 2));
                        binding.pinchCursor2.setX(x2 - ((float) binding.pinchCursor2.getWidth() / 2));
                        binding.pinchCursor2.setY(y2 - ((float) binding.pinchCursor2.getHeight() / 2));
                        sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.PINCH, x1, x2, y1, y2));
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_CANCEL:
                        sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.PINCH, 0, 0, 0, 0));
                        binding.pinchCursor1.setVisibility(INVISIBLE);
                        binding.pinchCursor2.setVisibility(INVISIBLE);
                        anim1.scaleX(1.0F).scaleY(1.0F).setDuration(100).start();
                        anim2.scaleX(1.0F).scaleY(1.0F).setDuration(100).start();
                        break;
                }
            }
        } else {
            // Reset if less than two pointers are detected
            binding.pinchCursor1.setVisibility(INVISIBLE);
            binding.pinchCursor2.setVisibility(INVISIBLE);
        }
    }


    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        HelperMethods.setUI(this);


        if (event.getToolType(0) == TOOL_TYPE_STYLUS) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_HOVER_MOVE) {
                touchMode = false;
                hideToolBar(true);

                if (showLastPointMode) {
                    binding.cursorImage.setX(x - ((float) binding.cursorImage.getWidth() / 2));
                    binding.cursorImage.setY(y - ((float) binding.cursorImage.getHeight() / 2));
                    binding.cursorImage.setVisibility(VISIBLE);
                }
                if (clientWidth > 0 && clientHeight > 0) {
                    float scaledX = x * (1920.0f / clientWidth);
                    float scaledY = y * (1080.0f / clientHeight);
                    sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.HOVER, (int) scaledX, (int) scaledY, 0.01f));
                } else {
                    float x1 = x * (1920.0f / 2560.0f);
                    float y1 = y * (1080.0f / 1600.0f);
                    sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.HOVER, (int) x1, (int) y1, 0.01f));
                }
            }
            else if (action == MotionEvent.ACTION_HOVER_ENTER) {
                hideToolBar(true);
                binding.drawActivity.setPadding(0,0 , 0, 0);
            }
            else{
                touchMode = touchMode && lastTouchMode;
                hideToolBar(false);
                binding.drawActivity.setPadding(0,paddingTopInPixels , 0, 0);
            }
        }


        return super.onGenericMotionEvent(event);
    }

    private void hideToolBar(boolean hide) {
        int visibility = hide ? GONE : VISIBLE;
        binding.ToolButton.setVisibility(visibility);

        if (hide) {
            binding.leftToolbar.setVisibility(GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void initDefaultFloatingButtons(){
        binding.undoButton.setOnClickListener(v ->{
            sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.HOTKEY, new KeyHelper.KeyCode[]{KeyHelper.KeyCode.LControl,KeyHelper.KeyCode.Z}));
        });
        binding.redoButton.setOnClickListener(v ->{
            sendQueue.offer(HelperMethods.sendData(HelperMethods.ACTION.HOTKEY, new KeyHelper.KeyCode[]{KeyHelper.KeyCode.LControl,KeyHelper.KeyCode.Y}));
        });
        binding.pinchModeButton.setOnClickListener(v ->{
            touchMode =!touchMode;
            lastTouchMode = touchMode;
        });
       binding.showCursorButton.setOnClickListener(v ->{
           showLastPointMode=!showLastPointMode;
       });

    }






}
