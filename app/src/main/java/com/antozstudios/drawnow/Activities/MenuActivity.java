package com.antozstudios.drawnow.Activities;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ext.SdkExtensions;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.RequiresExtension;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.antozstudios.drawnow.Helper.Records.HostData;
import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.Manager.ProfileManager;
import com.antozstudios.drawnow.Manager.ServerHistoryManager;
import com.antozstudios.drawnow.databinding.ActivityMenuBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MenuActivity extends AppCompatActivity {

    private static final String TAG = "MenuActivity";
    public static final String SERVICE_TYPE = "_gerimo._tcp.";

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private LinearLayout linearLayout;
    private final Map<String,NsdServiceInfo> discoveredServices = new ConcurrentHashMap<>();

    ProfileManager profileManager;
    PrefManager prefManager;

    private ActivityMenuBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initManager();
        initTheme();

        binding.createProfilButton.setOnClickListener(view -> {
            startActivity(new Intent(this, CreateProfileActivity.class));
        });
        linearLayout = binding.hostsLinearLayout;

        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        if (nsdManager == null) {
            Log.e(TAG, "Failed to get NSD Manager!");
        }
        syncLoadingAnimation();

        
    }

    private void initTheme() {
        int theme = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG)
                .getInt(PrefManager.KeyPref.THEME.getKey(), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);
    }

    private void initManager() {
        prefManager = PrefManager.getInstance(this);
        profileManager = ProfileManager.getInstance(this);
    }

    private class MyResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed for " + serviceInfo.getServiceName() + ": " + errorCode);
        }

        @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 17)
        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "Service resolved: " + serviceInfo.getServiceName() + " @ " + serviceInfo.getHost());
            discoveredServices.put(serviceInfo.getServiceName(), serviceInfo);

            runOnUiThread(() -> addOrUpdateButton(serviceInfo));
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 17)
    private void addOrUpdateButton(NsdServiceInfo serviceInfo) {
        String serviceName = serviceInfo.getServiceName();
        // Prüfe, ob Button schon existiert
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            View child = linearLayout.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                if (serviceName.equals(button.getTag())) {
                    // Existiert, ggf. aktualisieren
                    return;
                }
            }
        }
        MaterialButton button = new MaterialButton(this);
        button.setTag(serviceName);
        button.setText(serviceName);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        button.setOnClickListener(v -> {
            if(profileManager.getAllProfileNames().isEmpty() ||
                    prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                            .getInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), -1) < 0) {
                Snackbar.make(binding.getRoot(), "Create or select a profile first", Snackbar.LENGTH_SHORT).show();
                return;
            }


            Intent intent = new Intent(MenuActivity.this, DrawActivity.class);
            String saveHostIP = serviceInfo.getHost().getHostAddress();

            int savePort = serviceInfo.getPort();

            intent.putExtra("com.antozstudios.drawnow.SERVER_IP", saveHostIP);
            intent.putExtra("com.antozstudios.drawnow.SERVER_PORT", savePort);

            prefManager.putDataPref(PrefManager.DataPref.SHOW_HOSTS)
                    .putString(PrefManager.KeyPref.LAST_IP.getKey(), saveHostIP)
                    .putInt(PrefManager.KeyPref.LAST_PORT.getKey(), savePort).commit();

            startActivity(intent);
        });
        linearLayout.addView(button);

        syncLoadingAnimation();
    }

    private void syncLoadingAnimation() {
        if (linearLayout.getChildCount() > 0) {
            binding.loadingAnimation.setVisibility(INVISIBLE);
            binding.loadingAnimation.cancelAnimation();
        } else {
            binding.loadingAnimation.setVisibility(VISIBLE);
            binding.loadingAnimation.playAnimation();
        }
    }

    private void removeButton(String serviceName) {
        for(int i = 0; i < linearLayout.getChildCount(); i++) {
            View child = linearLayout.getChildAt(i);
            if(child instanceof Button) {
                Button button = (Button) child;
                if(serviceName.equals(button.getTag())) {
                    linearLayout.removeView(button);
                    break;
                }
            }
        }
        syncLoadingAnimation();
    }

    private void startDiscovery() {
        if (nsdManager != null) {
            Log.d(TAG, "Starting service discovery...");
            initializeDiscoveryListener();
            discoveredServices.clear();
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } else {
            Log.e(TAG, "NSD Manager is null, cannot start discovery.");
        }
    }

    private void stopDiscovery() {
        if (nsdManager != null && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Discovery listener not registered, ignoring.", e);
            }
            discoveryListener = null;
        }
    }

    private void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service found: " + service.getServiceName());
                if (!discoveredServices.containsKey(service.getServiceName())) {
                    nsdManager.resolveService(service, new MyResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "Service lost: " + service.getServiceName());
                if(discoveredServices.containsKey(service.getServiceName())){
                    discoveredServices.remove(service.getServiceName());
                    runOnUiThread(() -> removeButton(service.getServiceName()));
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                stopDiscovery();
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Stop Discovery failed: Error code:" + errorCode);
            }
        };
    }

    private Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable restartDiscovery = new Runnable() {
        @Override
        public void run() {
            stopDiscovery();
            startDiscovery();
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        startDiscovery();
        handler.postDelayed(restartDiscovery, 2000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(restartDiscovery);
        stopDiscovery();
        super.onPause();
    }
}
