package com.antozstudios.gerimo.Activities;

import static android.view.MotionEvent.AXIS_TILT;
import static android.view.MotionEvent.TOOL_TYPE_FINGER;
import static android.view.MotionEvent.TOOL_TYPE_STYLUS;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;

import com.antozstudios.gerimo.Helper.HelperClass;
import com.antozstudios.gerimo.Helper.HelperMethods;
import com.antozstudios.gerimo.Helper.KeyHelper;
import com.antozstudios.gerimo.Helper.Records.ScreenData;
import com.antozstudios.gerimo.Manager.PrefManager;
import com.antozstudios.gerimo.Manager.ProfileManager;
import com.antozstudios.gerimo.R;
import com.antozstudios.gerimo.databinding.ActivityDrawBinding;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DrawActivity extends AppCompatActivity {

    public static final String EXTRA_SERVER_IP = "com.antozstudios.gerimo.SERVER_IP";
    private static final String TAG = "UDPClient";

    private String SERVER_IP;
    private int SERVER_PORT = 5000;
    private ActivityDrawBinding binding;

    private DatagramSocket udpSocket;
    private InetAddress serverAddress;

    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
    private Thread senderThread;
    private Thread receiverThread;

    boolean lastTouchMode, touchMode, showLastPointMode;

    ViewPropertyAnimator anim1, anim2;

    ProfileManager profileManager;

    volatile ArrayList<ScreenData> currentScreenData;
    int currentIndex;
    PrefManager prefManager;
    private int clientWidth;
    private int clientHeight;

    private boolean isOfflineMode = false;

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDrawBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            lastTouchMode = savedInstanceState.getBoolean("lastTouchMode");
            touchMode = savedInstanceState.getBoolean("touchMode");
            showLastPointMode = savedInstanceState.getBoolean("showLastPointMode");
        }

        binding.drawActivity.post(() -> {
            clientWidth = binding.drawActivity.getWidth();
            clientHeight = binding.drawActivity.getHeight();
        });

        prefManager = PrefManager.getInstance(this);
        profileManager = ProfileManager.getInstance(this);
        Log.d("mmm",profileManager.readFile());
        anim1 = binding.pinchCursor1.animate();
        anim2 = binding.pinchCursor2.animate();

        initDefaultFloatingButtons();
        initShortcutToolbar();

        binding.ToolButton.setOnClickListener(v -> {
            View[] viewsToToggle = {
                    binding.undoButton,
                    binding.redoButton,
                    binding.pinchModeButton,
                    binding.showCursorButton,
                    binding.settingsButton,
                    binding.openShortcutButton,
                    binding.shortcutToolbarButton
            };
            int newVisibility = binding.undoButton.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            for (View view : viewsToToggle) {
                view.setVisibility(newVisibility);
            }
        });

        binding.openShortcutButton.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateShortcutActivity.class));
        });
        binding.settingsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        String serverIpFromIntent = getIntent().getStringExtra(EXTRA_SERVER_IP);
        int serverPortFromIntent = getIntent().getIntExtra("com.antozstudios.gerimo.SERVER_PORT", 5000);

        if (serverIpFromIntent != null && !serverIpFromIntent.isEmpty()) {
            SERVER_IP = serverIpFromIntent;
            SERVER_PORT = serverPortFromIntent;
            sendQueue.offer(HelperMethods.sendEnquiry(HelperMethods.GET_ACTION.RESOLUTION));
        } else {
            isOfflineMode = true;
        }

        HelperMethods.setUI(this);
    }



    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("lastTouchMode", lastTouchMode);
        outState.putBoolean("touchMode", touchMode);
        outState.putBoolean("showLastPointMode", showLastPointMode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentIndex = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getInt(PrefManager.KeyPref.CURRENT_SCREEN_INDEX.getKey(), 0);
        applySettings();
        updateShortcutSpinner();
        if (!isOfflineMode) {
            initUDP();
        }
    }

    private void applySettings() {
        String toolbarPosition = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getString(PrefManager.KeyPref.CURRENT_TOOLBAR_POSITION.getKey(), "LEFT");

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(binding.drawActivity);

        if (toolbarPosition.equals("RIGHT")) {
            // Toolbar to the right
            constraintSet.connect(R.id.left_toolbar_scrollview, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            constraintSet.clear(R.id.left_toolbar_scrollview, ConstraintSet.START);

            // Spinner to the left of the toolbar
            constraintSet.connect(R.id.shortcut_profile_spinner, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            constraintSet.connect(R.id.shortcut_profile_spinner, ConstraintSet.END, R.id.left_toolbar_scrollview, ConstraintSet.START);

            // Right toolbar (shortcut buttons) to the left of the main toolbar
            constraintSet.connect(R.id.right_toolbar, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            constraintSet.connect(R.id.right_toolbar, ConstraintSet.END, R.id.left_toolbar_scrollview, ConstraintSet.START);

        } else {
            // Toolbar to the left
            constraintSet.connect(R.id.left_toolbar_scrollview, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            constraintSet.clear(R.id.left_toolbar_scrollview, ConstraintSet.END);

            // Spinner to the right of the toolbar
            constraintSet.connect(R.id.shortcut_profile_spinner, ConstraintSet.START, R.id.left_toolbar_scrollview, ConstraintSet.END);
            constraintSet.connect(R.id.shortcut_profile_spinner, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

            // Right toolbar (shortcut buttons) to the right of the main toolbar
            constraintSet.connect(R.id.right_toolbar, ConstraintSet.START, R.id.left_toolbar_scrollview, ConstraintSet.END);
            constraintSet.connect(R.id.right_toolbar, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        }

        constraintSet.applyTo(binding.drawActivity);
    }


    private void initUDP() {
        try {
            udpSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_IP);

            senderThread = new Thread(this::sendLoop);
            senderThread.start();

            receiverThread = new Thread(this::receiveLoop);
            receiverThread.start();

            Log.i(TAG, "UDP client started. Target: " + SERVER_IP + ":" + SERVER_PORT);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing UDP: ", e);
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.i(TAG, "Sender thread interrupted.");
        } catch (Exception e) {
            if (udpSocket != null && !udpSocket.isClosed()) {
                Log.e(TAG, "Error while sending: ", e);
            }
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                udpSocket.receive(packet);

                String[] received = new String(packet.getData(), 0, packet.getLength()).split(":");
                if (received.length < 2) continue; // Basic validation

                String monitorData = received[1];

                // Reverted to the original logic that worked reliably
                currentIndex = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getInt(PrefManager.KeyPref.CURRENT_SCREEN_INDEX.getKey(), 0);
                prefManager.putDataPref(PrefManager.DataPref.SETTINGS_CONFIG).putString(PrefManager.KeyPref.MONITOR_DATA.getKey(), monitorData).commit();
                currentScreenData = HelperClass.getAllScreenData(monitorData);

            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted() || (udpSocket != null && udpSocket.isClosed())) {
                    Log.i(TAG, "Receiver thread interrupted.");
                    break;
                } else {
                    Log.e(TAG, "Error while receiving: ", e);
                }
            } catch (Exception e) { // Catch other potential exceptions like IndexOutOfBounds
                 Log.e(TAG, "Error processing received data: ", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isOfflineMode) {
            closeUDP();
        }
    }

    private void closeUDP() {
        if (senderThread != null) senderThread.interrupt();
        if (receiverThread != null) receiverThread.interrupt();
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        Log.i(TAG, "UDP client closed");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isOfflineMode) return super.onTouchEvent(event);
        if (event.getToolType(0) == TOOL_TYPE_STYLUS) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            float pressure = event.getPressure() > 0 ? event.getPressure() : 0;
            float tilt = event.getAxisValue(AXIS_TILT);
            float orientation = event.getOrientation() > 0 ? event.getOrientation() : 0;
            float tiltX = (float) (Math.sin(orientation) * Math.toDegrees(tilt));
            float tiltY = (float) (Math.cos(orientation) * Math.toDegrees(tilt));

            if (showLastPointMode) {
                binding.cursorImage.setVisibility(VISIBLE);
                binding.cursorImage.setX(x - ((float) binding.cursorImage.getWidth() / 2));
                binding.cursorImage.setY(y - ((float) binding.cursorImage.getHeight() / 2));
                binding.cursorImage.setScaleX((float) Math.max(0.5, pressure));
                binding.cursorImage.setScaleY((float) Math.max(0.5, pressure));
            }

            if (clientWidth > 0 && clientHeight > 0 && currentScreenData != null && currentIndex < currentScreenData.size()) {
                float scaledX = (x * (currentScreenData.get(currentIndex).workareaWidth() / (float) clientWidth));
                float scaledY = (y * (currentScreenData.get(currentIndex).workareaHeight() / (float) clientHeight));

                sendQueue.offer(HelperMethods.sendData(HelperMethods.SET_ACTION.CLICK, (int) scaledX +currentScreenData.get(currentIndex).workareaX(), (int) scaledY+currentScreenData.get(currentIndex).workareaY(), pressure, (int) tiltX, (int) tiltY));
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
                    case MotionEvent.ACTION_MOVE:
                        binding.pinchCursor1.setX(x1 - ((float) binding.pinchCursor1.getWidth() / 2));
                        binding.pinchCursor1.setY(y1 - ((float) binding.pinchCursor1.getHeight() / 2));
                        binding.pinchCursor2.setX(x2 - ((float) binding.pinchCursor2.getWidth() / 2));
                        binding.pinchCursor2.setY(y2 - ((float) binding.pinchCursor2.getHeight() / 2));

                        if (clientWidth > 0 && clientHeight > 0 && currentScreenData != null && currentIndex < currentScreenData.size()) {
                            ScreenData screen = currentScreenData.get(currentIndex);
                            float scaledX1 = (x1 * (screen.workareaWidth() / (float) clientWidth)) + screen.workareaX();
                            float scaledY1 = (y1 * (screen.workareaHeight() / (float) clientHeight)) + screen.workareaY();
                            float scaledX2 = (x2 * (screen.workareaWidth() / (float) clientWidth)) + screen.workareaX();
                            float scaledY2 = (y2 * (screen.workareaHeight() / (float) clientHeight)) + screen.workareaY();
                            sendQueue.offer(HelperMethods.sendData(HelperMethods.SET_ACTION.PINCH, (int) scaledX1, (int) scaledX2, (int) scaledY1, (int) scaledY2));
                        } else {
                            sendQueue.offer(HelperMethods.sendData(HelperMethods.SET_ACTION.PINCH, x1, x2, y1, y2));
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_CANCEL:
                        sendQueue.offer(HelperMethods.sendData(HelperMethods.SET_ACTION.PINCH, 0, 0, 0, 0));
                        binding.pinchCursor1.setVisibility(INVISIBLE);
                        binding.pinchCursor2.setVisibility(INVISIBLE);
                        anim1.scaleX(1.0F).scaleY(1.0F).setDuration(100).start();
                        anim2.scaleX(1.0F).scaleY(1.0F).setDuration(100).start();
                        break;
                }
            }
        } else {
            binding.pinchCursor1.setVisibility(INVISIBLE);
            binding.pinchCursor2.setVisibility(INVISIBLE);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        HelperMethods.setUI(this);
        if (isOfflineMode) return super.onGenericMotionEvent(event);

        if (event.getToolType(0) == TOOL_TYPE_STYLUS) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_HOVER_MOVE) {
                touchMode = false;

                if (showLastPointMode) {
                    binding.cursorImage.setX(x - ((float) binding.cursorImage.getWidth() / 2));
                    binding.cursorImage.setY(y - ((float) binding.cursorImage.getHeight() / 2));
                    binding.cursorImage.setVisibility(VISIBLE);
                }
                if (clientWidth > 0 && clientHeight > 0 && currentScreenData != null && currentIndex < currentScreenData.size()) {
                    float scaledX = (x * (currentScreenData.get(currentIndex).workareaWidth() / (float) clientWidth));
                    float scaledY = (y * (currentScreenData.get(currentIndex).workareaHeight() / (float) clientHeight));
                    sendQueue.offer(HelperMethods.sendData(HelperMethods.SET_ACTION.HOVER, (int) scaledX + currentScreenData.get(currentIndex).workareaX(), (int) scaledY +currentScreenData.get(currentIndex).workareaY() , 0.01f));
                }
            } else if (action == MotionEvent.ACTION_HOVER_ENTER) {
                hideToolBar(false);
            } else {
                touchMode = touchMode && lastTouchMode;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void hideToolBar(boolean hide) {
        int visibility = hide ? GONE : VISIBLE;
        binding.ToolButton.setVisibility(visibility);
        binding.leftToolbar.setVisibility(visibility);

        if (hide) {
            binding.leftToolbar.setVisibility(GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isOfflineMode) {
            closeUDP();
        }
    }

    void initDefaultFloatingButtons() {
        binding.undoButton.setOnClickListener(v -> {
            if(!isOfflineMode) sendQueue.offer(HelperMethods.sendData(HelperMethods.SET_ACTION.HOTKEY, new KeyHelper.KeyCode[]{KeyHelper.KeyCode.LControl, KeyHelper.KeyCode.Z}));
        });
        binding.redoButton.setOnClickListener(v -> {
            if(!isOfflineMode) sendQueue.offer(HelperMethods.sendData(HelperMethods.SET_ACTION.HOTKEY, new KeyHelper.KeyCode[]{KeyHelper.KeyCode.LControl, KeyHelper.KeyCode.Y}));
        });
        binding.pinchModeButton.setOnClickListener(v -> {
            touchMode = !touchMode;
            lastTouchMode = touchMode;
        });
        binding.showCursorButton.setOnClickListener(v -> {
            if (showLastPointMode && binding.cursorImage.getVisibility() == VISIBLE) {
                binding.cursorImage.setVisibility(INVISIBLE);
            }
            showLastPointMode = !showLastPointMode;
        });
    }

    private void initShortcutToolbar() {
        binding.shortcutToolbarButton.setOnClickListener(v -> {
            int currentVisibility = binding.rightToolbar.getVisibility();
            int newVisibility = currentVisibility == VISIBLE ? GONE : VISIBLE;
            binding.rightToolbar.setVisibility(newVisibility);
            binding.shortcutProfileSpinner.setVisibility(newVisibility);
        });

        binding.shortcutProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProfile = (String) parent.getItemAtPosition(position);
                if (selectedProfile != null) {
                    prefManager.putDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                            .putString(PrefManager.KeyPref.CURRENT_SHORTPROFILE.getKey(), selectedProfile)
                            .commit();
                    binding.shortcutButtonsLayout.removeAllViews();
                    profileManager.loadShortcutButtons(DrawActivity.this, binding.shortcutButtonsLayout, selectedProfile, sendQueue);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                binding.shortcutButtonsLayout.removeAllViews();
            }
        });

        updateShortcutSpinner();

        binding.rightToolbar.setVisibility(View.GONE);
        binding.shortcutProfileSpinner.setVisibility(View.GONE);
    }

    private void updateShortcutSpinner() {
        String currentProfile = prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                .getString(PrefManager.KeyPref.CURRENT_PROFILE.toString(), "");

        ArrayAdapter<String> shortcutAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, profileManager.getShortcutProfileNames(currentProfile));
        shortcutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.shortcutProfileSpinner.setAdapter(shortcutAdapter);

        String lastSelectedProfile = prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG).getString(PrefManager.KeyPref.CURRENT_SHORTPROFILE.getKey(), "");
        if (!lastSelectedProfile.isEmpty()) {
            int spinnerPosition = shortcutAdapter.getPosition(lastSelectedProfile);
            if (spinnerPosition >= 0) {
                binding.shortcutProfileSpinner.setSelection(spinnerPosition);
            }
        }
    }
}
