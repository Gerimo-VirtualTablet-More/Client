package com.antozstudios.drawnow.Activities;

import static com.antozstudios.drawnow.Helper.JsonPayload.buildJsonPayload;
import static com.antozstudios.drawnow.Helper.KeyHelper.KeyCode.getAllKeys;
import static com.antozstudios.drawnow.Helper.KeyHelper.KeyCode.getValue;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.Manager.ProfileManager;
import com.antozstudios.drawnow.R;
import com.antozstudios.drawnow.databinding.PopupCreateShortcutsBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ShortcutActivity extends AppCompatActivity {

    SpinnerAdapter spinnerAdapter;
    PopupCreateShortcutsBinding popupCreateShortcutsBinding;
    PrefManager prefManager;
    ProfileManager profileManager;

    private String getCurrentProfileName() {
        return prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                .getString(PrefManager.KeyPref.CURRENT_PROFILE.toString(), "");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        popupCreateShortcutsBinding = PopupCreateShortcutsBinding.inflate(getLayoutInflater());
        setContentView(popupCreateShortcutsBinding.getRoot());

        prefManager = PrefManager.getInstance(this);
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getAllKeys());
        profileManager = ProfileManager.getInstance((AppCompatActivity) this);

        ArrayAdapter<String> getShortcutProfiles = new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, profileManager.getShortcutProfileNames(getCurrentProfileName()));

        popupCreateShortcutsBinding.shortcutProfileSpinner.setAdapter(getShortcutProfiles);

        String lastSelectedProfile = prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG).getString(PrefManager.KeyPref.CURRENT_SHORTPROFILE.getKey(), "");
        if (!lastSelectedProfile.isEmpty()) {
            int spinnerPosition = getShortcutProfiles.getPosition(lastSelectedProfile);
            if (spinnerPosition >= 0) {
                popupCreateShortcutsBinding.shortcutProfileSpinner.setSelection(spinnerPosition);
            }
        }

        popupCreateShortcutsBinding.createProfileButton.setOnClickListener((view) -> {
            String shortcutProfileInput = Objects.requireNonNull(popupCreateShortcutsBinding.shortcutProfileInput.getText()).toString().toLowerCase();
            if (!shortcutProfileInput.isEmpty()) {
                String currentProfile = getCurrentProfileName();
                if (profileManager.shortcutProfileExists(currentProfile, shortcutProfileInput)) {
                    runOnUiThread(() -> {
                        Snackbar.make(view, "Profile already exists", Snackbar.LENGTH_SHORT).show();
                    });
                    return;
                }

                profileManager.createShortcutProfile(currentProfile, shortcutProfileInput);

                ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        profileManager.getShortcutProfileNames(currentProfile)
                );
                popupCreateShortcutsBinding.shortcutProfileSpinner.setAdapter(updatedAdapter);

                int newPosition = updatedAdapter.getPosition(shortcutProfileInput);
                if (newPosition >= 0) {
                    popupCreateShortcutsBinding.shortcutProfileSpinner.setSelection(newPosition);
                }
                popupCreateShortcutsBinding.shortcutProfileInput.setText("");
            } else {
                view.post(() -> {
                    Snackbar.make(view, "Please enter a name", Snackbar.LENGTH_SHORT).show();
                });
            }
        });

        popupCreateShortcutsBinding.deleteProfileButton.setOnClickListener((view) -> {
            if (popupCreateShortcutsBinding.shortcutProfileSpinner.getSelectedItem() == null) {
                Snackbar.make(view, "No profile selected to delete.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            String getSpinnerValue = popupCreateShortcutsBinding.shortcutProfileSpinner.getSelectedItem().toString();
            String currentProfile = getCurrentProfileName();
            if (profileManager.getShortcutProfile(currentProfile, getSpinnerValue) != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Shortcut Profile")
                        .setMessage("Are you sure you want to delete the Shortcut Profile '" + getSpinnerValue + "'?")
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            profileManager.deleteShortcutProfile(currentProfile, getSpinnerValue);
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) popupCreateShortcutsBinding.shortcutProfileSpinner.getAdapter();
                            adapter.clear();
                            adapter.addAll(profileManager.getShortcutProfileNames(currentProfile));
                            adapter.notifyDataSetChanged();
                            Snackbar.make(view, "Shortcut Profile '" + getSpinnerValue + "' deleted.", Snackbar.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else {
                view.post(() -> {
                    Snackbar.make(view, "Shortcut Profile not found", Snackbar.LENGTH_SHORT).show();
                });
            }
        });

        popupCreateShortcutsBinding.shortcutProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getItemAtPosition(position) != null) {
                    String selectedProfile = parent.getItemAtPosition(position).toString();
                    prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG).edit().putString(PrefManager.KeyPref.CURRENT_SHORTPROFILE.getKey(), selectedProfile).apply();
                    Log.d("Value of Spinner", selectedProfile);
                    loadShortcutsForProfile(selectedProfile);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                LinearLayout sequenceLayout = popupCreateShortcutsBinding.seqeuenceLinearlayout;
                int childCount = sequenceLayout.getChildCount();
                if (childCount > 2) {
                    sequenceLayout.removeViews(2, childCount - 2);
                }
            }
        });

        popupCreateShortcutsBinding.saveShortcutProfileButton.setOnClickListener((view) -> {
            if (popupCreateShortcutsBinding.shortcutProfileSpinner.getSelectedItem() == null) {
                Snackbar.make(view, "Please select a profile to save to.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            String selectedProfile = popupCreateShortcutsBinding.shortcutProfileSpinner.getSelectedItem().toString();
            String currentProfile = getCurrentProfileName();

            if (!profileManager.shortcutProfileExists(currentProfile, selectedProfile)) {
                Snackbar.make(view, "Profile '" + selectedProfile + "' not found.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            LinearLayout sequenceLayout = popupCreateShortcutsBinding.seqeuenceLinearlayout;
            JSONObject newShortcutData = new JSONObject();

            for (int i = 2; i < sequenceLayout.getChildCount(); i++) {
                View keySequenceView = sequenceLayout.getChildAt(i);
                TextInputEditText commandNameEditText = keySequenceView.findViewById(R.id.keySequence_MainLinearLayout).findViewById(R.id.shortcutProfileName);
                String commandName = commandNameEditText.getText().toString().trim();

                if (commandName.isEmpty()) {
                    commandName = "Command"; // Use default name if empty
                }

                LinearLayout spinnerLayout = keySequenceView.findViewById(R.id.spinnerLinearLayout);
                StringBuilder commandSequence = new StringBuilder();
                for (int j = 0; j < spinnerLayout.getChildCount(); j++) {
                    View child = spinnerLayout.getChildAt(j);
                    if (child instanceof Spinner) {
                        Spinner spinner = (Spinner) child;
                        if (spinner.getSelectedItem() != null) {
                            String key = spinner.getSelectedItem().toString();
                            if (!key.equalsIgnoreCase("none")) {
                                if (commandSequence.length() > 0) {
                                    commandSequence.append(";");
                                }
                                commandSequence.append(key);
                            }
                        }
                    }
                }

                if (commandSequence.length() > 0) {
                    String uniqueKey = commandName + "___" + UUID.randomUUID().toString();
                    try {
                        newShortcutData.put(uniqueKey, commandSequence.toString());
                    } catch (JSONException e) {
                        Log.e("ShortcutActivity", "Error creating shortcut JSON", e);
                        Snackbar.make(view, "Error saving shortcut data.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            profileManager.updateShortcutProfile(currentProfile, selectedProfile, newShortcutData);
            Snackbar.make(view, "Shortcuts saved to profile '" + selectedProfile + "'.", Snackbar.LENGTH_SHORT).show();
        });

        popupCreateShortcutsBinding.addKeysequenceButton.setOnClickListener((v1) -> createAndAddShortcutSection_View());
        popupCreateShortcutsBinding.aiSearchButton.setOnClickListener((v2) -> {
            String searchText = Objects.requireNonNull(popupCreateShortcutsBinding.aiSearchInput.getText()).toString();
            if (!searchText.isEmpty()) {
                popupCreateShortcutsBinding.loadingAnimation.setVisibility(View.VISIBLE);
                popupCreateShortcutsBinding.aiSearchInput.setText("");
                new Thread(() -> aiSearch(searchText)).start();
            }
        });
    }

    private void loadShortcutsForProfile(String profileName) {
        LinearLayout sequenceLayout = popupCreateShortcutsBinding.seqeuenceLinearlayout;
        int childCount = sequenceLayout.getChildCount();
        if (childCount > 2) {
            sequenceLayout.removeViews(2, childCount - 2);
        }

        String currentProfile = getCurrentProfileName();
        JSONObject shortcuts = profileManager.getShortcutProfile(currentProfile, profileName);
        if (shortcuts == null) {
            return;
        }

        Iterator<String> keys = shortcuts.keys();
        while (keys.hasNext()) {
            String uniqueKey = keys.next();
            String commandName = uniqueKey.contains("___") ? uniqueKey.split("___")[0] : uniqueKey;
            String commandSequence = shortcuts.optString(uniqueKey);

            if (commandName.isEmpty() || commandSequence.isEmpty()) {
                continue;
            }

            View keySequenceView = createAndAddShortcutSection_View();
            TextInputEditText commandNameEditText = keySequenceView.findViewById(R.id.keySequence_MainLinearLayout).findViewById(R.id.shortcutProfileName);
            commandNameEditText.setText(commandName);

            LinearLayout spinnerLayout = keySequenceView.findViewById(R.id.spinnerLinearLayout);
            spinnerLayout.removeAllViews();

            String[] commands = commandSequence.split(";");
            for (String command : commands) {
                if (!command.isEmpty()) {
                    Spinner spinner = createSpinner(keySequenceView, new Spinner(this));
                    int selectionIndex = getValue(command);
                    if (selectionIndex >= 0) {
                        spinner.setSelection(selectionIndex);
                    }
                }
            }
        }
    }

    private void addShortcuts(JSONObject jsonObject) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject children = jsonObject.optJSONObject(key);
            Iterator<String> childKeys = Objects.requireNonNull(children).keys();
            while (childKeys.hasNext()) {
                String commandName = childKeys.next().toLowerCase();
                String[] commands = children.optString(commandName).split(";");
                View tempSection = createAndAddShortcutSection_View();

                for (String command : commands) {
                    Spinner spinner = createSpinner(tempSection, new Spinner(this));
                    spinner.setSelection(getValue(command));
                }
                TextInputEditText textInputEditText = tempSection.findViewById(R.id.keySequence_MainLinearLayout).findViewById(R.id.shortcutProfileName);
                textInputEditText.setText(commandName);
                popupCreateShortcutsBinding.shortcutProfileInput.setText(key);
            }
        }
    }

    public View createAndAddShortcutSection_View() {
        View key_sequence = getLayoutInflater().inflate(R.layout.key_sequence, null);
        Button addSpinnerButton = key_sequence.findViewById(R.id.addSpinnerButton);
        Button deleteSpinnerButton = key_sequence.findViewById(R.id.deleteSpinnerButton);
        addSpinnerButton.setOnClickListener((v2) -> {
            Spinner spinner = new Spinner(this);
            spinner.setAdapter(spinnerAdapter);
            createSpinner(key_sequence, spinner);
        });
        deleteSpinnerButton.setOnClickListener((v2) -> {
            LinearLayout view = key_sequence.findViewById(R.id.spinnerLinearLayout);
            if (view.getChildCount() > 0) {
                view.removeViewAt(view.getChildCount() - 1);
            }
        });
        key_sequence.findViewById(R.id.deleteButton).setOnClickListener((view) -> {
            popupCreateShortcutsBinding.seqeuenceLinearlayout.removeView(key_sequence);
        });

        popupCreateShortcutsBinding.seqeuenceLinearlayout.addView(key_sequence);
        return key_sequence;
    }

    public Spinner createSpinner(View tempKeySequence, Spinner s) {
        s.setAdapter(spinnerAdapter);
        LinearLayout spinnerLinearLayout = tempKeySequence.findViewById(R.id.spinnerLinearLayout);
        spinnerLinearLayout.addView(s);
        return s;
    }

    void aiSearch(String text) {
        if (!text.isEmpty()) {
            String jsonBody = null;
            try {
                jsonBody = buildJsonPayload(text, "deepseek/deepseek-chat-v3.1:free");
            } catch (JSONException e) {
                Log.d(ShortcutActivity.class.getName(), "Error creating JSON payload.");
            }
            if (jsonBody == null) {
                Log.d(ShortcutActivity.class.getName(), "JsonBody is null.");
                popupCreateShortcutsBinding.getRoot().post(() -> popupCreateShortcutsBinding.loadingAnimation.setVisibility(View.GONE));
                return;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + prefManager.getDataPref(PrefManager.DataPref.SETTINGS_CONFIG).getString(PrefManager.KeyPref.API_KEY.getKey(), ""))
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(ShortcutActivity.class.getName(), "Error at Request: " + e.getMessage());
                    popupCreateShortcutsBinding.getRoot().post(() -> popupCreateShortcutsBinding.loadingAnimation.setVisibility(View.GONE));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        if (!responseBody.isEmpty() && !responseBody.equals("{}")) {
                            try {
                                JSONObject jsonObject = new JSONObject(responseBody);
                                JSONArray choices = jsonObject.getJSONArray("choices");
                                JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                                String reasoningString = message.getString("reasoning");
                                if (reasoningString.contains("{")) {
                                    int startIndex = reasoningString.indexOf("{");
                                    int endIndex = reasoningString.lastIndexOf("}");
                                    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                                        reasoningString = reasoningString.substring(startIndex, endIndex + 1);
                                        final JSONObject reasoning = new JSONObject(reasoningString);
                                        popupCreateShortcutsBinding.getRoot().post(() -> {
                                            addShortcuts(reasoning);
                                        });
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(ShortcutActivity.class.getName(), "Error parsing JSON: " + e.getMessage());
                            }
                        }
                    } else {
                        Log.e(ShortcutActivity.class.getName(), "Request failed: " + response.code() + " " + responseBody);

                        runOnUiThread(() -> {
                            Snackbar.make(popupCreateShortcutsBinding.getRoot(), "Request failed: " + response.code() + " " + responseBody, Snackbar.LENGTH_SHORT).show();
                        });

                    }
                    popupCreateShortcutsBinding.getRoot().post(() -> popupCreateShortcutsBinding.loadingAnimation.setVisibility(View.GONE));
                }
            });
        }
    }
}
