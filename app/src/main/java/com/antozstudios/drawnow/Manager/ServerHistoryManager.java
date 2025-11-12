package com.antozstudios.drawnow.Manager;

import android.content.Context;
import android.util.Log;

import com.antozstudios.drawnow.Helper.Records.HostData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ServerHistoryManager {

    private static final String TAG = "ServerHistoryManager";
    private static final String FILE_NAME = "ServerHistory.json";

    private final Context context;
    private final Gson gson;
    private static ServerHistoryManager instance;

    private List<HostData> serverHistory;

    public static synchronized ServerHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new ServerHistoryManager(context.getApplicationContext());
        }
        return instance;
    }

    private ServerHistoryManager(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.serverHistory = loadServerHistory();
    }

    public List<HostData> getServerHistory() {
        return new ArrayList<>(serverHistory); // Return a copy
    }

    public void saveServer(HostData newServer) {
        // Remove existing server to avoid duplicates and handle updates.
        serverHistory.removeIf(s -> s.hostAddress().equals(newServer.hostAddress()) && s.port() == newServer.port());
        serverHistory.add(newServer);
        saveServerHistoryToFile();
    }

    public void deleteServer(HostData serverToDelete) {
        if (serverHistory.removeIf(s -> s.equals(serverToDelete))) {
            saveServerHistoryToFile();
        }
    }

    private List<HostData> loadServerHistory() {
        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            Type type = new TypeToken<ArrayList<HostData>>() {}.getType();
            List<HostData> loadedHistory = gson.fromJson(reader, type);
            return loadedHistory != null ? loadedHistory : new ArrayList<>();

        } catch (IOException e) {
            Log.i(TAG, "No server history file found, creating a new list.");
            return new ArrayList<>();
        }
    }

    private void saveServerHistoryToFile() {
        String json = gson.toJson(serverHistory);
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Error saving server history to file", e);
        }
    }
}
