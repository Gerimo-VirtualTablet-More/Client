package com.antozstudios.drawnow.Manager;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProfileManager {

    private final Context context;
    private final String fileName = "Profiles.json";
    private static ProfileManager instance;
    private JSONObject jsonObject;

    public static synchronized ProfileManager getInstance(AppCompatActivity appCompatActivity) {
        if (instance == null) {
            instance = new ProfileManager(appCompatActivity);
        }
        return instance;
    }

    private ProfileManager(Context context) {
        this.context = context;
        init();
    }

    public void init() {
        File file = context.getFileStreamPath(fileName);
        if (file == null || !file.exists()) {
            // File does not exist, create a new empty structure
            try {
                jsonObject = new JSONObject();
                JSONObject profiles = new JSONObject();
                jsonObject.put("profiles", profiles);
                saveJson();
            } catch (JSONException e) {
                Log.e("ProfileManager", "Error creating initial JSON structure", e);
            }
        } else {
            // File exists, read it
            try {
                String jsonString = readFile();
                if (jsonString.isEmpty()) {
                    jsonObject = new JSONObject();
                    jsonObject.put("profiles", new JSONObject());
                } else {
                    jsonObject = new JSONObject(jsonString);
                }
            } catch (JSONException e) {
                Log.e("ProfileManager", "Error parsing existing JSON file", e);
                jsonObject = new JSONObject();
                try {
                    jsonObject.put("profiles", new JSONObject());
                } catch (JSONException jsonException) {}
            }
        }
    }

    public String exportProfiles() {
        try {
            return jsonObject.toString(4);
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error exporting profiles to string", e);
            return null;
        }
    }

    public boolean importProfiles(String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            return false;
        }
        try {
            JSONObject importedJson = new JSONObject(jsonContent);
            if (!importedJson.has("profiles")) {
                Log.e("ProfileManager", "Import failed: JSON does not contain 'profiles' key.");
                return false;
            }
            this.jsonObject = importedJson;
            saveJson();
            return true;
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error parsing imported JSON content", e);
            return false;
        }
    }

    public List<String> getAllProfileNames() {
        List<String> profileNames = new ArrayList<>();
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            Iterator<String> keys = profiles.keys();
            while (keys.hasNext()) {
                profileNames.add(keys.next());
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Could not retrieve profile names", e);
        }
        return profileNames;
    }

    public List<String> getShortcutProfileNames(String mainProfile) {
        List<String> names = new ArrayList<>();
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(mainProfile)) {
                JSONObject profile = profiles.getJSONObject(mainProfile);
                if (profile.has("shortCutProfiles")) {
                    JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
                    Iterator<String> keys = shortcutProfiles.keys();
                    while (keys.hasNext()) {
                        names.add(keys.next());
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Could not retrieve shortcut profile names", e);
        }
        return names;
    }

    public void createProfile(String name) {
        try {
            if (name == null || name.trim().isEmpty()) return;
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (!profiles.has(name)) {
                JSONObject newProfile = new JSONObject();
                newProfile.put("shortCutProfiles", new JSONObject());
                profiles.put(name, newProfile);
                saveJson();
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error creating profile", e);
        }
    }

    public void deleteProfile(String profileName) {
        if (profileName == null) {
            Log.w("ProfileManager", "Attempted to delete a null profile.");
            return;
        }
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(profileName)) {
                profiles.remove(profileName);
                saveJson();
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error deleting profile", e);
        }
    }

    //---------- NEUE CONVENIENCE-METHODEN ----------

    // ShortcutProfile anlegen
    public void createShortcutProfile(String profileName, String shortcutProfileName) {
        if (shortcutProfileName == null || shortcutProfileName.trim().isEmpty()) {
            Log.w("ProfileManager", "Shortcut profile name cannot be empty");
            return;
        }
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (!profiles.has(profileName)) createProfile(profileName);
            JSONObject profile = profiles.getJSONObject(profileName);
            JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
            if (!shortcutProfiles.has(shortcutProfileName)) {
                shortcutProfiles.put(shortcutProfileName, new JSONObject());
                saveJson();
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error creating shortcut profile", e);
        }
    }

    // ShortcutProfile löschen
    public void deleteShortcutProfile(String profileName, String shortcutProfileName) {
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(profileName)) {
                JSONObject profile = profiles.getJSONObject(profileName);
                JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
                if (shortcutProfiles.has(shortcutProfileName)) {
                    shortcutProfiles.remove(shortcutProfileName);
                    saveJson();
                }
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error deleting shortcut profile", e);
        }
    }

    // ShortcutProfile umbenennen
    public boolean renameShortcutProfile(String profileName, String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty()) return false;
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(profileName)) {
                JSONObject profile = profiles.getJSONObject(profileName);
                JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
                if (shortcutProfiles.has(oldName) && !shortcutProfiles.has(newName)) {
                    JSONObject shortcutProfile = shortcutProfiles.getJSONObject(oldName);
                    shortcutProfiles.put(newName, shortcutProfile);
                    shortcutProfiles.remove(oldName);
                    saveJson();
                    return true;
                }
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error renaming shortcut profile", e);
        }
        return false;
    }

    // ShortcutProfile-Existenz prüfen
    public boolean shortcutProfileExists(String profileName, String shortcutProfileName) {
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(profileName)) {
                JSONObject profile = profiles.getJSONObject(profileName);
                JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
                return shortcutProfiles.has(shortcutProfileName);
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error checking if shortcut profile exists", e);
        }
        return false;
    }

    // ShortcutProfile abrufen
    public JSONObject getShortcutProfile(String profileName, String shortcutProfileName) {
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(profileName)) {
                JSONObject profile = profiles.getJSONObject(profileName);
                JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
                if (shortcutProfiles.has(shortcutProfileName)) {
                    return shortcutProfiles.getJSONObject(shortcutProfileName);
                }
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error getting shortcut profile", e);
        }
        return null;
    }

    // ShortcutProfile Befehle als Map abrufen
    public Map<String, String> getShortcutCommands(String profileName, String shortcutProfileName) {
        Map<String, String> commands = new HashMap<>();
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(profileName)) {
                JSONObject profile = profiles.getJSONObject(profileName);
                JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
                if (shortcutProfiles.has(shortcutProfileName)) {
                    JSONObject shortcutProfile = shortcutProfiles.getJSONObject(shortcutProfileName);
                    Iterator<String> keys = shortcutProfile.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        commands.put(key, shortcutProfile.getString(key));
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error retrieving shortcut commands", e);
        }
        return commands;
    }

    // Einzelnen Shortcut zu einem ShortcutProfile hinzufügen/aktualisieren
    public void addCommandToShortcut(String profileName, String shortcutProfileName,
                                     String commandName, String commandSequence) {
        if (commandName == null || commandName.trim().isEmpty()
                || commandSequence == null || commandSequence.trim().isEmpty()) {
            Log.w("ProfileManager", "Command name and sequence cannot be empty");
            return;
        }
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (!profiles.has(profileName)) createProfile(profileName);
            JSONObject profile = profiles.getJSONObject(profileName);
            JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
            if (!shortcutProfiles.has(shortcutProfileName))
                shortcutProfiles.put(shortcutProfileName, new JSONObject());
            JSONObject shortcutProfile = shortcutProfiles.getJSONObject(shortcutProfileName);
            shortcutProfile.put(commandName, commandSequence);
            saveJson();
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error adding command to shortcut", e);
        }
    }

    // Einzelnen Shortcut aus ShortcutProfile löschen
    public void removeCommandFromShortcut(String profileName, String shortcutProfileName, String commandName) {
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(profileName)) {
                JSONObject profile = profiles.getJSONObject(profileName);
                JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
                if (shortcutProfiles.has(shortcutProfileName)) {
                    JSONObject shortcutProfile = shortcutProfiles.getJSONObject(shortcutProfileName);
                    if (shortcutProfile.has(commandName)) {
                        shortcutProfile.remove(commandName);
                        saveJson();
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error removing command from shortcut", e);
        }
    }

    public boolean profileExists(String profileName) {
        try {
            if (profileName == null || profileName.trim().isEmpty()) return false;
            return jsonObject.getJSONObject("profiles").has(profileName);
        } catch (JSONException e) {
            return false;
        }
    }

    private void saveJson() {
        try (FileOutputStream fos = context.openFileOutput(fileName, MODE_PRIVATE)) {
            fos.write(jsonObject.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException e) {
            Log.e("ProfileManager", "Error saving JSON to file", e);
        }
    }

    public String readFile() {
        StringBuilder stringBuilder = new StringBuilder();
        try (FileInputStream fileInputStream = context.openFileInput(fileName);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            Log.i("ProfileManager", "Could not read file: " + e.getMessage());
        }
        return stringBuilder.toString();
    }

    // Profile umbenennen (optional)
    public boolean renameProfile(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty()) return false;
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(oldName) && !profiles.has(newName)) {
                JSONObject profile = profiles.getJSONObject(oldName);
                profiles.put(newName, profile);
                profiles.remove(oldName);
                saveJson();
                return true;
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error renaming profile", e);
        }
        return false;
    }

    // Zugriff auf den gesamten JSON-Objekt für fortgeschrittene Nutzung
    public JSONObject getRawJsonObject() {
        return jsonObject;
    }
}
