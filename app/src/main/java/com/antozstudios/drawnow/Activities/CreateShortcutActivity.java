package com.antozstudios.drawnow.Activities;

import static com.antozstudios.drawnow.Helper.JsonPayload.buildJsonPayload;
import static com.antozstudios.drawnow.Helper.KeyHelper.KeyCode.getAllKeys;
import static com.antozstudios.drawnow.Helper.KeyHelper.KeyCode.getValue;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.Manager.ProfileManager;
import com.antozstudios.drawnow.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.antozstudios.drawnow.databinding.ActivityShortcutBinding;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateShortcutActivity extends AppCompatActivity {

    SpinnerAdapter spinnerAdapter;
    ActivityShortcutBinding activityShortcutBinding;

    PrefManager prefManager;
    ProfileManager profileManager;

    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;
    private String mJsonToExport;

    private Handler hintHandler = new Handler();
    private List<String> hintList;
    private int hintIndex = 0;

    private String getCurrentProfileName() {
        return prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                .getString(PrefManager.KeyPref.CURRENT_PROFILE.toString(), "");
    }

    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityShortcutBinding = ActivityShortcutBinding.inflate(getLayoutInflater());
        setContentView(activityShortcutBinding.getRoot());

        prefManager = PrefManager.getInstance(this);
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getAllKeys());
        profileManager = ProfileManager.getInstance((AppCompatActivity) this);

        registerActivityResults();

        setupHintSwitcher();

        activityShortcutBinding.keySequenceCard.setVisibility(View.GONE);
        activityShortcutBinding.aiModeCard.setVisibility(View.GONE);

        ArrayAdapter<String> getShortcutProfiles = new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, profileManager.getShortcutProfileNames(getCurrentProfileName()));

        activityShortcutBinding.shortcutProfileSpinner.setAdapter(getShortcutProfiles);

        String lastSelectedProfile = prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG).getString(PrefManager.KeyPref.CURRENT_SHORTPROFILE.getKey(), "");
        if (!lastSelectedProfile.isEmpty()) {
            int spinnerPosition = getShortcutProfiles.getPosition(lastSelectedProfile);
            if (spinnerPosition >= 0) {
                activityShortcutBinding.shortcutProfileSpinner.setSelection(spinnerPosition);
            }
        }

        activityShortcutBinding.manageProfileButton.setOnClickListener(this::showManageProfileMenu);
        activityShortcutBinding.importButton.setOnClickListener(v -> handleImport());
        activityShortcutBinding.exportButton.setOnClickListener(v -> handleExport());

        activityShortcutBinding.shortcutProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getItemAtPosition(position) != null) {
                    activityShortcutBinding.keySequenceCard.setVisibility(View.VISIBLE);
                    activityShortcutBinding.aiModeCard.setVisibility(View.VISIBLE);
                    String selectedProfile = parent.getItemAtPosition(position).toString();
                    prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG).edit().putString(PrefManager.KeyPref.CURRENT_SHORTPROFILE.getKey(), selectedProfile).apply();
                    Log.d("Value of Spinner", selectedProfile);
                    loadShortcutsForProfile(selectedProfile);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                activityShortcutBinding.keySequenceCard.setVisibility(View.GONE);
                activityShortcutBinding.aiModeCard.setVisibility(View.GONE);
                LinearLayout sequenceLayout = activityShortcutBinding.seqeuenceLinearlayout;
                int childCount = sequenceLayout.getChildCount();
                if (childCount > 2) {
                    sequenceLayout.removeViews(2, childCount - 2);
                }
            }
        });

        activityShortcutBinding.saveShortcutProfileButton.setOnClickListener((view) -> {
            if (activityShortcutBinding.shortcutProfileSpinner.getSelectedItem() == null) {
                Snackbar.make(view, "Please select a profile to save to.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            String selectedProfile = activityShortcutBinding.shortcutProfileSpinner.getSelectedItem().toString();
            String currentProfile = getCurrentProfileName();

            if (!profileManager.shortcutProfileExists(currentProfile, selectedProfile)) {
                Snackbar.make(view, "Profile '" + selectedProfile + "' not found.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            LinearLayout sequenceLayout = activityShortcutBinding.seqeuenceLinearlayout;
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
                    if (child instanceof Spinner spinner) {
                        if (spinner.getSelectedItem() != null) {
                            String key = spinner.getSelectedItem().toString();
                            if (!key.equalsIgnoreCase("none")) {
                                if (!commandSequence.isEmpty()) {
                                    commandSequence.append(";");
                                }
                                commandSequence.append(key);
                            }
                        }
                    }
                }

                if (!commandSequence.isEmpty()) {
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

        activityShortcutBinding.addKeysequenceButton.setOnClickListener((v1) -> createAndAddShortcutSection_View());
        activityShortcutBinding.aiSearchButton.setOnClickListener((v2) -> {
            String searchText = Objects.requireNonNull(activityShortcutBinding.aiSearchInput.getText()).toString();
            if (!searchText.isEmpty()) {
                activityShortcutBinding.loadingAnimation.setVisibility(View.VISIBLE);
                activityShortcutBinding.aiSearchInput.setText("");
                new Thread(() -> aiSearch(searchText)).start();
            }
        });
    }

    private void setupHintSwitcher() {
        try {
            Resources res = getResources();
            hintList = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.ai_search_hints)));
            Collections.shuffle(hintList);
            hintHandler.post(hintRunnable);
        } catch (Resources.NotFoundException e) {
            Log.e("CreateShortcutActivity", "Hint array not found", e);
        }
    }

    private final Runnable hintRunnable = new Runnable() {
        @Override
        public void run() {
            if (hintList != null && !hintList.isEmpty()) {
                activityShortcutBinding.aiSearchInputLayout.setHint(hintList.get(hintIndex));
                hintIndex = (hintIndex + 1) % hintList.size();
                hintHandler.postDelayed(this, 3000); // Switch every 3 seconds
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hintHandler.removeCallbacks(hintRunnable); // Prevent memory leaks
    }

    private void showManageProfileMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.manage_profile_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_create_profile) {
                handleCreateProfile();
                return true;
            } else if (itemId == R.id.action_edit_profile) {
                handleEditProfile();
                return true;
            } else if (itemId == R.id.action_delete_profile) {
                handleDeleteProfile();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void handleCreateProfile() {
        final EditText input = new EditText(this);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.create)
                .setMessage(R.string.enter_a_name)
                .setView(input)
                .setPositiveButton(R.string.create, (dialog, which) -> {
                    String shortcutProfileInput = input.getText().toString().toLowerCase().trim();
                    if (!shortcutProfileInput.isEmpty()) {
                        String currentProfile = getCurrentProfileName();
                        if (profileManager.shortcutProfileExists(currentProfile, shortcutProfileInput)) {
                            Snackbar.make(activityShortcutBinding.getRoot(), R.string.already_exists, Snackbar.LENGTH_SHORT).show();
                            return;
                        }

                        profileManager.createShortcutProfile(currentProfile, shortcutProfileInput);

                        ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(
                                this,
                                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                                profileManager.getShortcutProfileNames(currentProfile)
                        );
                        activityShortcutBinding.shortcutProfileSpinner.setAdapter(updatedAdapter);

                        int newPosition = updatedAdapter.getPosition(shortcutProfileInput);
                        if (newPosition >= 0) {
                            activityShortcutBinding.shortcutProfileSpinner.setSelection(newPosition);
                        }
                        Snackbar.make(activityShortcutBinding.getRoot(), getString(R.string.profile) + shortcutProfileInput + getString(R.string.created) + ".", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(activityShortcutBinding.getRoot(), R.string.please_enter_a_name, Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void handleEditProfile() {
        if (activityShortcutBinding.shortcutProfileSpinner.getSelectedItem() == null) {
            Snackbar.make(activityShortcutBinding.getRoot(), "No profile selected to edit.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String oldName = activityShortcutBinding.shortcutProfileSpinner.getSelectedItem().toString();
        final EditText input = new EditText(this);
        input.setText(oldName);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Profile Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().toLowerCase().trim();
                    if (newName.isEmpty() || newName.equals(oldName)) {
                        return;
                    }

                    String currentProfile = getCurrentProfileName();
                    if (profileManager.shortcutProfileExists(currentProfile, newName)) {
                        Snackbar.make(activityShortcutBinding.getRoot(), "A profile with this name already exists.", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    profileManager.renameShortcutProfile(currentProfile, oldName, newName);

                    ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(
                            this,
                            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                            profileManager.getShortcutProfileNames(currentProfile)
                    );
                    activityShortcutBinding.shortcutProfileSpinner.setAdapter(updatedAdapter);

                    int newPosition = updatedAdapter.getPosition(newName);
                    if (newPosition >= 0) {
                        activityShortcutBinding.shortcutProfileSpinner.setSelection(newPosition);
                    }

                    Snackbar.make(activityShortcutBinding.getRoot(), "Profile renamed to '" + newName + "'.", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void handleDeleteProfile() {
        if (activityShortcutBinding.shortcutProfileSpinner.getSelectedItem() == null) {
            Snackbar.make(activityShortcutBinding.getRoot(), "No profile selected to delete.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String getSpinnerValue = activityShortcutBinding.shortcutProfileSpinner.getSelectedItem().toString();
        String currentProfile = getCurrentProfileName();
        if (profileManager.getShortcutProfile(currentProfile, getSpinnerValue) != null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Shortcut Profile")
                    .setMessage("Are you sure you want to delete the Shortcut Profile '" + getSpinnerValue + "'?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        profileManager.deleteShortcutProfile(currentProfile, getSpinnerValue);
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) activityShortcutBinding.shortcutProfileSpinner.getAdapter();
                        adapter.clear();
                        adapter.addAll(profileManager.getShortcutProfileNames(currentProfile));
                        adapter.notifyDataSetChanged();
                        Snackbar.make(activityShortcutBinding.getRoot(), "Shortcut Profile '" + getSpinnerValue + "' deleted.", Snackbar.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            Snackbar.make(activityShortcutBinding.getRoot(), "Shortcut Profile not found", Snackbar.LENGTH_SHORT).show();
        }
    }


    private void loadShortcutsForProfile(String profileName) {
        LinearLayout sequenceLayout = activityShortcutBinding.seqeuenceLinearlayout;
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

    private void handleImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        importLauncher.launch(intent);
    }

    private void handleExport() {
        String currentProfile = getCurrentProfileName();
        List<String> shortcutProfileNames = profileManager.getShortcutProfileNames(currentProfile);

        if (shortcutProfileNames.isEmpty()) {
            Snackbar.make(activityShortcutBinding.getRoot(), "No shortcut profiles to export.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        showMultiChoiceDialog(shortcutProfileNames, "Select profiles to export", selectedProfiles -> {
            if (selectedProfiles.isEmpty()) {
                Snackbar.make(activityShortcutBinding.getRoot(), "No profiles selected for export.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            mJsonToExport = profileManager.exportShortcutProfiles(currentProfile, selectedProfiles);
            if (mJsonToExport != null) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "exportedShortcuts.json");
                exportLauncher.launch(intent);
            }
        });
    }

    private void registerActivityResults() {
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                                if (outputStream != null && mJsonToExport != null) {
                                    outputStream.write(mJsonToExport.getBytes());
                                    Snackbar.make(activityShortcutBinding.getRoot(), "Export successful!", Snackbar.LENGTH_SHORT).show();
                                } else {
                                    throw new IOException("OutputStream is null or nothing to export");
                                }
                            } catch (IOException e) {
                                Snackbar.make(activityShortcutBinding.getRoot(), "Export failed: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                Log.e("ExportShortcutProfile", "File write failed", e);
                            } finally {
                                mJsonToExport = null; // Clear after use
                            }
                        }
                    }
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                                 BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    stringBuilder.append(line);
                                }
                            } catch (IOException e) {
                                Snackbar.make(activityShortcutBinding.getRoot(), "Import failed: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                return;
                            }

                            String jsonContent = stringBuilder.toString();
                            List<String> availableProfiles = profileManager.getShortcutProfileNamesFromContent(jsonContent);

                            showMultiChoiceDialog(availableProfiles, "Select profiles to import", selectedProfiles -> {
                                if (selectedProfiles.isEmpty()) {
                                    Snackbar.make(activityShortcutBinding.getRoot(), "No profiles selected for import.", Snackbar.LENGTH_SHORT).show();
                                    return;
                                }

                                String currentProfile = getCurrentProfileName();
                                if (profileManager.importShortcutProfiles(currentProfile, jsonContent, selectedProfiles)) {
                                    Snackbar.make(activityShortcutBinding.getRoot(), "Import successful!", Snackbar.LENGTH_SHORT).show();
                                    ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(
                                            this,
                                            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                                            profileManager.getShortcutProfileNames(currentProfile)
                                    );
                                    activityShortcutBinding.shortcutProfileSpinner.setAdapter(updatedAdapter);
                                } else {
                                    Snackbar.make(activityShortcutBinding.getRoot(), "Import failed. Invalid file format.", Snackbar.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
    }

    private void showMultiChoiceDialog(List<String> items, String title, OnProfilesSelectedListener listener) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String item : items) {
            CheckBox checkBox = (CheckBox) getLayoutInflater().inflate(R.layout.checkbox_shortcut, layout, false);
            checkBox.setText(item);
            layout.addView(checkBox);
            checkBoxes.add(checkBox);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    List<String> selectedItems = new ArrayList<>();
                    for (CheckBox checkBox : checkBoxes) {
                        if (checkBox.isChecked()) {
                            selectedItems.add(checkBox.getText().toString());
                        }
                    }
                    listener.onProfilesSelected(selectedItems);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    interface OnProfilesSelectedListener {
        void onProfilesSelected(List<String> selectedProfiles);
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
                activityShortcutBinding.shortcutProfileInput.setText(key);
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
            activityShortcutBinding.seqeuenceLinearlayout.removeView(key_sequence);
        });

        activityShortcutBinding.seqeuenceLinearlayout.addView(key_sequence);
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
                Log.d(CreateShortcutActivity.class.getName(), "Error creating JSON payload.");
            }
            if (jsonBody == null) {
                Log.d(CreateShortcutActivity.class.getName(), "JsonBody is null.");
                activityShortcutBinding.getRoot().post(() -> activityShortcutBinding.loadingAnimation.setVisibility(View.GONE));
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
                    Log.e(CreateShortcutActivity.class.getName(), "Error at Request: " + e.getMessage());
                    runOnUiThread(() -> {
                        activityShortcutBinding.loadingAnimation.setVisibility(View.GONE);
                        Snackbar.make(activityShortcutBinding.getRoot(), "Request failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseBody = response.body().string();
                    runOnUiThread(() -> activityShortcutBinding.loadingAnimation.setVisibility(View.GONE));
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
                                        runOnUiThread(() -> addShortcuts(reasoning));
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(CreateShortcutActivity.class.getName(), "Error parsing JSON: " + e.getMessage());
                                runOnUiThread(() -> Snackbar.make(activityShortcutBinding.getRoot(), "Error processing AI response.", Snackbar.LENGTH_LONG).show());
                            }
                        }
                    } else {
                        Log.e(CreateShortcutActivity.class.getName(), "Request failed: " + response.code() + " " + responseBody);
                        String errorMessage;
                        if (response.code() == 429) {
                            errorMessage = "Rate limit reached. Please try again later.";
                        } else {
                            errorMessage = "Request failed: " + response.code();
                        }
                        runOnUiThread(() -> Snackbar.make(activityShortcutBinding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show());
                    }
                }
            });
        }
    }
}
