package com.antozstudios.drawnow.Activities;

import static com.google.android.material.R.attr.materialButtonOutlinedStyle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.antozstudios.drawnow.Helper.Records.HostData;
import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.Manager.ProfileManager;
import com.antozstudios.drawnow.Manager.ServerHistoryManager;
import com.antozstudios.drawnow.databinding.ActivityMenuBinding;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MenuActivity extends AppCompatActivity {

    private static final String TAG = "MenuActivity";
    public static final String SERVICE_TYPE = "_gerimo._tcp.";

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private LinearLayout linearLayout;
    private ServerHistoryManager serverHistoryManager;
    private final Map<String, NsdServiceInfo> discoveredServices = new ConcurrentHashMap<>();
    private final Set<String> lostServices = Collections.newSetFromMap(new ConcurrentHashMap<>());

    ProfileManager profileManager;
    PrefManager prefManager;


    private class MyResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed for " + serviceInfo.getServiceName() + ": " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "Service resolved: " + serviceInfo.getServiceName() + " @ " + serviceInfo.getHost());

            if (lostServices.contains(serviceInfo.getServiceName())) {
                Log.d(TAG, "Ignored resolution for a service that was already lost.");
                return;
            }

            discoveredServices.put(serviceInfo.getServiceName(), serviceInfo);
            runOnUiThread(MenuActivity.this::updateHostList);
        }
    }

    private void updateHostList() {
        linearLayout.removeAllViews();



        // 1. Add servers from history
        Set<HostData> combinedHosts = new LinkedHashSet<>(serverHistoryManager.getServerHistory());

        // 2. Add newly discovered services
        for (NsdServiceInfo serviceInfo : discoveredServices.values()) {
            combinedHosts.add(new HostData(serviceInfo.getServiceName(), serviceInfo.getHost().getHostAddress(), serviceInfo.getPort()));
        }



        // 3. Update the UI with host buttons
        for (HostData host : combinedHosts) {
            MaterialButton button = new MaterialButton(this);


            button.setText(host.hostName() != null ? host.hostName() : host.hostAddress());

            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                   new AlertDialog.Builder(v.getContext()).setTitle("Delete saved host.")
                           .setMessage("Are you sure you want to delete this host?")
                           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           Snackbar.make(v, "Host deleted", Snackbar.LENGTH_SHORT).show();
                           linearLayout.removeView(v);
                           serverHistoryManager.deleteServer(host);

                       }
                   }).setNegativeButton("Cancel",null).show();

                    return true;
                }
            });

            button.setOnClickListener(v -> {



                if(profileManager.getAllProfileNames().isEmpty() || prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG).getInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), -1)<0){
                    Snackbar.make(MenuActivity.this, binding.getRoot(), "Create or select a profile first", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                button.setEnabled(false);

                // Save the selected server to the history
                serverHistoryManager.saveServer(host);

                // Start the DrawActivity
                Intent intent = new Intent(MenuActivity.this, DrawActivity.class);
                intent.putExtra("com.antozstudios.drawnow.SERVER_IP", host.hostAddress());
                intent.putExtra("com.antozstudios.drawnow.SERVER_PORT", host.port());

                prefManager.putDataPref(PrefManager.DataPref.SHOW_HOSTS).putString(PrefManager.KeyPref.LAST_IP.getKey(), host.hostAddress())
                        .putInt(PrefManager.KeyPref.LAST_PORT.getKey(), host.port()).commit();



                startActivity(intent);
            });
            linearLayout.addView(button);
        }
        MaterialButton offlineButton =null;
        if(combinedHosts.isEmpty()){
            // Add Offline Mode Button

                 offlineButton = new MaterialButton(this);
            offlineButton.setText("Continue without host.");
            offlineButton.setOnClickListener(v -> {
                if (profileManager.getAllProfileNames().isEmpty() || prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG).getInt(PrefManager.KeyPref.CURRENT_INDEX_PROFILE.getKey(), -1) < 0) {
                    Snackbar.make(binding.getRoot(), "Create or select a profile first", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MenuActivity.this, DrawActivity.class);
                startActivity(intent);
            });
            linearLayout.addView(offlineButton);
        }else{
            if(offlineButton!=null)
                linearLayout.removeView(offlineButton);
        }
    }

    private void startDiscovery() {
        if (nsdManager != null) {
            Log.d(TAG, "Starting service discovery...");
            initializeDiscoveryListener();

            discoveredServices.clear();
            lostServices.clear();

            // Immediately display saved servers before starting network discovery
            updateHostList();

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
                nsdManager.resolveService(service, new MyResolveListener());
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "Service lost: " + service.getServiceName());
                lostServices.add(service.getServiceName());
                if (discoveredServices.remove(service.getServiceName()) != null) {
                    runOnUiThread(MenuActivity.this::updateHostList);
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

    private ActivityMenuBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefManager = PrefManager.getInstance(this);

        profileManager = ProfileManager.getInstance(this);

        Log.d("aaaaaa",profileManager.readFile());

        int theme = prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getInt(PrefManager.KeyPref.THEME.getKey(), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);

        // Always set up the UI first, so the user doesn't see a blank screen.
        setupUI();
        linearLayout = binding.hostsLinearLayout;

        serverHistoryManager = ServerHistoryManager.getInstance(this);
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        if (nsdManager == null) {
            Log.e(TAG, "Failed to get NSD Manager!");
        }
    }

    private void setupUI() {
        binding = ActivityMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.createProfilButton.setOnClickListener(view -> {
            startActivity(new Intent(this, CreateProfileActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startDiscovery();
    }

    @Override
    protected void onPause() {
        if (nsdManager != null) { // Check if nsdManager is initialized
            stopDiscovery();
        }
        super.onPause();
    }
}
