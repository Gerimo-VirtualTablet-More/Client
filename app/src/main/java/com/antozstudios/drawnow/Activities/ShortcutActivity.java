package com.antozstudios.drawnow.Activities;

import static com.antozstudios.drawnow.Helper.JsonPayload.buildJsonPayload;
import static com.antozstudios.drawnow.Helper.KeyHelper.KeyCode.getAllKeys;
import static com.antozstudios.drawnow.Helper.KeyHelper.KeyCode.getValue;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.antozstudios.drawnow.Manager.PrefManager;
import com.antozstudios.drawnow.Manager.ProfileManager;
import com.antozstudios.drawnow.R;
import com.antozstudios.drawnow.databinding.PopupCreateShortcutsBinding;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

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
    private ValueAnimator animator;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        popupCreateShortcutsBinding = PopupCreateShortcutsBinding.inflate(getLayoutInflater());
        setContentView(popupCreateShortcutsBinding.getRoot());

        prefManager = PrefManager.getInstance(this);
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getAllKeys());
        profileManager = ProfileManager.getInstance((AppCompatActivity) this);

        String currentProfile = prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                .getString(PrefManager.KeyPref.CURRENT_PROFILE.toString(),"");

        ArrayAdapter<String> getShortcutProfiles = new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,profileManager.getShortcutProfileNames(currentProfile));


        popupCreateShortcutsBinding.shortcutProfileSpinner.setAdapter(getShortcutProfiles);

        popupCreateShortcutsBinding.createProfileButton.setOnClickListener((view) -> {
            String profileInput = Objects.requireNonNull(popupCreateShortcutsBinding.shortcutProfileInput.getText()).toString();
            if (!profileInput.isEmpty()) {
                profileManager.createShortcutProfile(currentProfile, profileInput);

                // Adapter mit der neuen, aktualisierten Liste von Profilen aktualisieren
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) popupCreateShortcutsBinding.shortcutProfileSpinner.getAdapter();
                adapter.clear();
                adapter.addAll(profileManager.getShortcutProfileNames(currentProfile));
                adapter.notifyDataSetChanged();

                // Optional: Das Eingabefeld leeren
                popupCreateShortcutsBinding.shortcutProfileInput.setText("");
            }else{
                view.post(()->{
                    Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
                });
            }
        });

        popupCreateShortcutsBinding.deleteProfileButton.setOnClickListener((view) -> {
            if (popupCreateShortcutsBinding.shortcutProfileSpinner.getSelectedItem() == null) {
                Toast.makeText(this, "No profile selected to delete.", Toast.LENGTH_SHORT).show();
                return;
            }
            String getSpinnerValue = popupCreateShortcutsBinding.shortcutProfileSpinner.getSelectedItem().toString();
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
                            Toast.makeText(this, "Shortcut Profile '" + getSpinnerValue + "' deleted.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else {
                view.post(() -> {
                    Toast.makeText(this, "Shortcut Profile not found", Toast.LENGTH_SHORT).show();
                });
            }
        });

        popupCreateShortcutsBinding.shortcutProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getItemAtPosition(position) != null) {
                    String selectedProfile = parent.getItemAtPosition(position).toString();
                    Log.d("Value of Spinner", selectedProfile);
                    loadShortcutsForProfile(selectedProfile);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Clear the shortcut views if no profile is selected
                LinearLayout sequenceLayout = popupCreateShortcutsBinding.seqeuenceLinearlayout;
                int childCount = sequenceLayout.getChildCount();
                if (childCount > 2) {
                    sequenceLayout.removeViews(2, childCount - 2);
                }
            }
        });

        popupCreateShortcutsBinding.saveShortcutProfileButton.setOnClickListener((view) -> {
            if (popupCreateShortcutsBinding.shortcutProfileSpinner.getSelectedItem() == null) {
                Toast.makeText(this, "Please select a profile to save to.", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedProfile = popupCreateShortcutsBinding.shortcutProfileSpinner.getSelectedItem().toString();


            if (!profileManager.shortcutProfileExists(currentProfile, selectedProfile)) {
                // This case should ideally not happen if the spinner is populated correctly
                Toast.makeText(this, "Profile '" + selectedProfile + "' not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            // To ensure a clean save, we delete and re-create the profile.
            // This handles renamed or deleted commands properly.
            profileManager.deleteShortcutProfile(currentProfile, selectedProfile);
            profileManager.createShortcutProfile(currentProfile, selectedProfile);

            LinearLayout sequenceLayout = popupCreateShortcutsBinding.seqeuenceLinearlayout;
            // Start from 2 to skip the static header and button layout
            for (int i = 2; i < sequenceLayout.getChildCount(); i++) {
                View keySequenceView = sequenceLayout.getChildAt(i);
                TextInputEditText commandNameEditText = keySequenceView.findViewById(R.id.keySequence_MainLinearLayout).findViewById(R.id.shortcutProfileName);
                String commandName = commandNameEditText.getText().toString().trim();

                if (commandName.isEmpty()) {
                    continue; // Skip sections without a command name
                }

                LinearLayout spinnerLayout = keySequenceView.findViewById(R.id.spinnerLinearLayout);
                StringBuilder commandSequence = new StringBuilder();
                for (int j = 0; j < spinnerLayout.getChildCount(); j++) {
                    View child = spinnerLayout.getChildAt(j);
                    if (child instanceof Spinner) {
                        Spinner spinner = (Spinner) child;
                        if (spinner.getSelectedItem() != null) {
                            String key = spinner.getSelectedItem().toString();
                            if (commandSequence.length() > 0) {
                                commandSequence.append(";");
                            }
                            commandSequence.append(key);
                        }
                    }
                }

                if (commandSequence.length() > 0) {
                    profileManager.addCommandToShortcut(currentProfile, selectedProfile, commandName, commandSequence.toString());
                }
            }

            Toast.makeText(this, "Shortcuts saved to profile '" + selectedProfile + "'.", Toast.LENGTH_SHORT).show();
        });


        initAnimation();

        popupCreateShortcutsBinding.addKeysequenceButton.setOnClickListener((v1)-> {createAndAddShortcutSection_View();});
        popupCreateShortcutsBinding.aiSearchButton.setOnClickListener((v2)->{
            String searchText = Objects.requireNonNull(popupCreateShortcutsBinding.aiSearchInput.getText()).toString();
            if (!searchText.isEmpty()) {
                popupCreateShortcutsBinding.aiSearchInput.setText("");
                new Thread(() -> {
                    aiSearch(searchText);
                    popupCreateShortcutsBinding.getRoot().post(() -> {
                        animator.setDuration(500);
                    });
                }).start();
            }
        });

    }



    private void loadShortcutsForProfile(String profileName) {
        // Clear existing dynamic shortcut views
        LinearLayout sequenceLayout = popupCreateShortcutsBinding.seqeuenceLinearlayout;
        int childCount = sequenceLayout.getChildCount();
        if (childCount > 2) {
            sequenceLayout.removeViews(2, childCount - 2);
        }

        String currentProfile = prefManager.getDataPref(PrefManager.DataPref.PROFILE_CONFIG)
                .getString(PrefManager.KeyPref.CURRENT_PROFILE.toString(),"");

        // Load shortcuts for the selected profile
        JSONObject shortcuts = profileManager.getShortcutProfile(currentProfile, profileName);
        if (shortcuts == null) {
            return;
        }

        // Create and populate views for each shortcut
        Iterator<String> keys = shortcuts.keys();
        while(keys.hasNext()) {
            String commandName = keys.next();
            String commandSequence = shortcuts.optString(commandName);

            if (commandName.isEmpty() || commandSequence.isEmpty()) {
                continue;
            }

            View keySequenceView = createAndAddShortcutSection_View();

            TextInputEditText commandNameEditText = keySequenceView.findViewById(R.id.keySequence_MainLinearLayout).findViewById(R.id.shortcutProfileName);
            commandNameEditText.setText(commandName);

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


    private void initAnimation() {
        View view = findViewById(R.id.aiLinearLayout);
        LayerDrawable layerDrawable = (LayerDrawable) view.getBackground();
        GradientDrawable borderGradient = (GradientDrawable) layerDrawable.getDrawable(0);
        int colorA = 0xFFFF5555;
        int colorB = 0xFF1100FF;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(a -> {
            float progress = (float) a.getAnimatedValue();
            int start = blend(colorA, colorB, progress);
            int end = blend(colorB, colorA, progress);
            borderGradient.setColors(new int[]{start, end});
        });
        animator.start();
    }

    private int blend(int from, int to, float ratio) {
        int r = (int) (Color.red(from) * (1 - ratio) + Color.red(to) * ratio);
        int g = (int) (Color.green(from) * (1 - ratio) + Color.green(to) * ratio);
        int b = (int) (Color.blue(from) * (1 - ratio) + Color.blue(to) * ratio);
        int a = (int) (Color.alpha(from) * (1 - ratio) + Color.alpha(to) * ratio);
        return Color.argb(a, r, g, b);
    }

    private void addShortcuts(JSONObject jsonObject){
        Iterator<String> keys = jsonObject.keys();
        while(keys.hasNext()){
            String key = keys.next();
            JSONObject children = jsonObject.optJSONObject(key);
            Iterator<String> childKeys = Objects.requireNonNull(children).keys();
            while (childKeys.hasNext()) {
                String commandName = childKeys.next();
                String[] commands = children.optString(commandName).split(";");
                View tempSection = createAndAddShortcutSection_View();

                for (String command : commands) {

                    Spinner spinner = createSpinner(tempSection,new Spinner(this));
                    spinner.setSelection(getValue(command));

                }
                TextInputEditText textInputEditText = tempSection
                        .findViewById(R.id.keySequence_MainLinearLayout)
                        .findViewById(R.id.shortcutProfileName);
                textInputEditText.setText(commandName);
                popupCreateShortcutsBinding.shortcutProfileInput.setText(key);
            }
        }
    }



    public View createAndAddShortcutSection_View(){
        View key_sequence = getLayoutInflater().inflate(R.layout.key_sequence, null);
        Button addSpinnerButton = key_sequence.findViewById(R.id.addSpinnerButton);
        Button deleteSpinnerButton = key_sequence.findViewById(R.id.deleteSpinnerButton);
        //-- Buttons for adding and deleting spinners --//
        addSpinnerButton.setOnClickListener((v2)->{
            Spinner spinner = new Spinner(this);
            spinner.setAdapter(spinnerAdapter);
            createSpinner(key_sequence,spinner);
        });
        deleteSpinnerButton.setOnClickListener((v2)-> {

            LinearLayout view = key_sequence.findViewById(R.id.spinnerLinearLayout);
            if(view.getChildCount()>0){
                view.removeViewAt(view.getChildCount()-1);
            }

        });
        key_sequence.findViewById(R.id.deleteButton).setOnClickListener((view)->{
            popupCreateShortcutsBinding.seqeuenceLinearlayout.removeView(key_sequence);
        });


        popupCreateShortcutsBinding.seqeuenceLinearlayout.addView(key_sequence);
        return key_sequence;
    }

    public Spinner createSpinner(View tempKeySequence,Spinner s){
        s.setAdapter(spinnerAdapter);
        LinearLayout spinnerLinearLayout = tempKeySequence.findViewById(R.id.spinnerLinearLayout);
        spinnerLinearLayout.addView(s);
        return s;
    }

    void aiSearch(String text){
        if (!text.isEmpty()) {
            String jsonBody = null;
            try {
                jsonBody = buildJsonPayload(text,"deepseek/deepseek-chat-v3.1:free");
            } catch (JSONException e) {
                Log.d(ShortcutActivity.class.getName(),"Error creating JSON payload.");
            }
            if(jsonBody==null) {
                Log.d(ShortcutActivity.class.getName(),"JsonBody is null.");
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
                }
                @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
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
                    }
                }
            });
        }
    }
}
