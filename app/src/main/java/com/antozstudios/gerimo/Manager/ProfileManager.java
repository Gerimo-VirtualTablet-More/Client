package com.antozstudios.gerimo.Manager;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.gridlayout.widget.GridLayout;

import com.antozstudios.gerimo.Helper.HelperMethods;
import com.antozstudios.gerimo.Helper.KeyHelper;
import com.antozstudios.gerimo.R;
import com.google.android.material.button.MaterialButton;

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
import java.util.concurrent.BlockingQueue;

public class ProfileManager {

    private final Context context;
    private final String fileName = "Profiles.json";
    private static ProfileManager instance;
    private JSONObject jsonObject;
    PrefManager prefManager;

    public static synchronized ProfileManager getInstance(AppCompatActivity appCompatActivity) {
        if (instance == null) {
            instance = new ProfileManager(appCompatActivity);
        }
        return instance;
    }

    private ProfileManager(Context context) {
        this.context = context;
        init();
        prefManager = PrefManager.getInstance(context);
    }

    public void init() {
        File file = context.getFileStreamPath(fileName);
        if (file == null || !file.exists()) {
            try {
                jsonObject = new JSONObject();
                JSONObject profiles = new JSONObject();
                jsonObject.put("profiles", profiles);
                saveJson();
            } catch (JSONException e) {
                Log.e("ProfileManager", "Error creating initial JSON structure", e);
            }
        } else {
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
                } catch (JSONException ignored) {
                }
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

    public void updateShortcutProfile(String profileName, String shortcutProfileName, JSONObject newShortcutData) {
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(profileName)) {
                JSONObject profile = profiles.getJSONObject(profileName);
                JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");
                shortcutProfiles.put(shortcutProfileName, newShortcutData);
                saveJson();
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error updating shortcut profile", e);
        }
    }

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
            return jsonObject.getJSONObject("profiles").has(profileName.toLowerCase());
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

    public String exportShortcutProfiles(String mainProfile, List<String> shortcutProfileNames) {
        try {
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (profiles.has(mainProfile)) {
                JSONObject profile = profiles.getJSONObject(mainProfile);
                if (profile.has("shortCutProfiles")) {
                    JSONObject allShortcutProfiles = profile.getJSONObject("shortCutProfiles");
                    JSONObject selectedShortcutProfiles = new JSONObject();
                    for (String name : shortcutProfileNames) {
                        if (allShortcutProfiles.has(name)) {
                            selectedShortcutProfiles.put(name, allShortcutProfiles.getJSONObject(name));
                        }
                    }
                    return selectedShortcutProfiles.toString(4);
                }
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error exporting shortcut profiles to string", e);
        }
        return null;
    }

    public boolean importShortcutProfiles(String mainProfile, String jsonContent, List<String> shortcutProfileNamesToImport) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            return false;
        }
        try {
            JSONObject importedJson = new JSONObject(jsonContent);
            JSONObject profiles = jsonObject.getJSONObject("profiles");
            if (!profiles.has(mainProfile)) {
                createProfile(mainProfile);
            }
            JSONObject profile = profiles.getJSONObject(mainProfile);
            JSONObject shortcutProfiles = profile.getJSONObject("shortCutProfiles");

            for (String name : shortcutProfileNamesToImport) {
                if (importedJson.has(name)) {
                    shortcutProfiles.put(name, importedJson.getJSONObject(name));
                }
            }
            saveJson();
            return true;
        } catch (JSONException e) {
            Log.e("ProfileManager", "Error parsing imported JSON content", e);
            return false;
        }
    }

    public List<String> getShortcutProfileNamesFromContent(String jsonContent) {
        List<String> names = new ArrayList<>();
        try {
            JSONObject importedJson = new JSONObject(jsonContent);
            Iterator<String> keys = importedJson.keys();
            while (keys.hasNext()) {
                names.add(keys.next());
            }
        } catch (JSONException e) {
            Log.e("ProfileManager", "Could not retrieve shortcut profile names from content", e);
        }
        return names;
    }


    public JSONObject getRawJsonObject() {
        return jsonObject;
    }

    public void loadShortcutButtons(Context context, GridLayout shortcutButtonsLayout, String profileName, BlockingQueue<String> sendQueue) {
        shortcutButtonsLayout.removeAllViews();

        String currentProfile = prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                .getString(PrefManager.KeyPref.CURRENT_PROFILE.toString(), "");

        JSONObject shortcuts = getShortcutProfile(currentProfile, profileName);
        if (shortcuts == null) {
            return;
        }

        Iterator<String> keys = shortcuts.keys();
        while (keys.hasNext()) {
            String uniqueKey = keys.next();
            String commandName = uniqueKey.split("___")[0]; // Extract real name
            String commandSequence = shortcuts.optString(uniqueKey);

            MaterialButton shortcutButton = new MaterialButton(new ContextThemeWrapper(context, R.style.Widget_App_MaterialButton_Outlined));
            shortcutButton.setText(commandName);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            int margin = (int) (4 * context.getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin, margin, margin);
            shortcutButton.setLayoutParams(params);

            shortcutButton.setOnClickListener(v -> {
                String[] keyStrings = commandSequence.split(";");
                List<KeyHelper.KeyCode> keyCodes = new ArrayList<>();
                for (String key : keyStrings) {
                    try {
                        keyCodes.add(KeyHelper.KeyCode.valueOf(key));
                    } catch (IllegalArgumentException e) {
                        Log.e("ProfileManager", "Invalid key code in shortcut: " + key, e);
                    }
                }
                if (!keyCodes.isEmpty()) {
                    sendQueue.offer(HelperMethods.sendData(HelperMethods.SET_ACTION.HOTKEY, keyCodes.toArray(new KeyHelper.KeyCode[0])));
                }
            });

            shortcutButtonsLayout.addView(shortcutButton);
        }
    }
}
